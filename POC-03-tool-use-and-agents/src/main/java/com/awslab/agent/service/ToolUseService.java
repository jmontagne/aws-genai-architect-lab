package com.awslab.agent.service;

import com.awslab.agent.config.AgentProperties;
import com.awslab.agent.exception.AgentException;
import com.awslab.agent.model.AgentResponse;
import com.awslab.agent.model.ToolDefinitions;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeAsyncClient;
import software.amazon.awssdk.services.bedrockruntime.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class ToolUseService {

    private static final Logger log = LoggerFactory.getLogger(ToolUseService.class);

    private final BedrockRuntimeAsyncClient bedrockClient;
    private final FlightTool flightTool;
    private final AgentProperties properties;
    private final ObjectMapper objectMapper;

    public ToolUseService(BedrockRuntimeAsyncClient bedrockClient,
                          FlightTool flightTool,
                          AgentProperties properties,
                          ObjectMapper objectMapper) {
        this.bedrockClient = bedrockClient;
        this.flightTool = flightTool;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public CompletableFuture<AgentResponse> execute(String userMessage, int maxIterations, float temperature) {
        long startTime = System.currentTimeMillis();
        List<Message> messages = new ArrayList<>();
        messages.add(Message.builder()
                .role(ConversationRole.USER)
                .content(ContentBlock.fromText(userMessage))
                .build());

        List<AgentResponse.ToolCall> toolCalls = new ArrayList<>();

        return converseLoop(messages, toolCalls, 0, maxIterations, temperature, startTime);
    }

    private CompletableFuture<AgentResponse> converseLoop(
            List<Message> messages,
            List<AgentResponse.ToolCall> toolCalls,
            int iteration,
            int maxIterations,
            float temperature,
            long startTime) {

        if (iteration >= maxIterations) {
            return CompletableFuture.failedFuture(
                    new AgentException(AgentException.ErrorCode.MAX_ITERATIONS_EXCEEDED,
                            "Exceeded maximum iterations: " + maxIterations));
        }

        log.debug("[Iteration {}] Calling Converse API with {} messages", iteration + 1, messages.size());

        ConverseRequest request = ConverseRequest.builder()
                .modelId(properties.getModelId())
                .messages(messages)
                .toolConfig(ToolConfiguration.builder()
                        .tools(ToolDefinitions.allTools())
                        .build())
                .inferenceConfig(InferenceConfiguration.builder()
                        .temperature(temperature)
                        .maxTokens(properties.getMaxTokens())
                        .build())
                .build();

        return bedrockClient.converse(request)
                .thenCompose(response -> {
                    StopReason stopReason = response.stopReason();
                    log.debug("[Iteration {}] stopReason={}", iteration + 1, stopReason);

                    if (stopReason == StopReason.END_TURN) {
                        String answer = extractText(response);
                        long latency = System.currentTimeMillis() - startTime;
                        return CompletableFuture.completedFuture(
                                new AgentResponse(answer, iteration + 1, toolCalls, latency, null));
                    }

                    if (stopReason == StopReason.TOOL_USE) {
                        return handleToolUse(response, messages, toolCalls, iteration, maxIterations,
                                temperature, startTime);
                    }

                    String answer = extractText(response);
                    long latency = System.currentTimeMillis() - startTime;
                    return CompletableFuture.completedFuture(
                            new AgentResponse(answer, iteration + 1, toolCalls, latency, null));
                })
                .exceptionally(ex -> {
                    if (ex.getCause() instanceof AgentException) {
                        throw (AgentException) ex.getCause();
                    }
                    throw new AgentException(AgentException.ErrorCode.CONVERSE_API_FAILED,
                            "Converse API failed: " + ex.getMessage(), ex);
                });
    }

    private CompletableFuture<AgentResponse> handleToolUse(
            ConverseResponse response,
            List<Message> messages,
            List<AgentResponse.ToolCall> toolCalls,
            int iteration,
            int maxIterations,
            float temperature,
            long startTime) {

        // Add assistant message (with tool use blocks) to history
        messages.add(response.output().message());

        // Find all tool use blocks in the response
        List<ContentBlock> contentBlocks = response.output().message().content();
        List<CompletableFuture<ToolResultBlock>> toolResultFutures = new ArrayList<>();

        for (ContentBlock block : contentBlocks) {
            if (block.toolUse() != null) {
                ToolUseBlock toolUse = block.toolUse();
                String toolName = toolUse.name();
                String toolInput = toolUse.input().toString();
                String toolUseId = toolUse.toolUseId();

                log.debug("[Iteration {}] Tool call: {}({})", iteration + 1, toolName, toolInput);
                toolCalls.add(new AgentResponse.ToolCall(toolName, toolInput));

                CompletableFuture<ToolResultBlock> resultFuture = executeTool(toolName, toolUse.input())
                        .thenApply(result -> ToolResultBlock.builder()
                                .toolUseId(toolUseId)
                                .content(ToolResultContentBlock.builder()
                                        .text(result)
                                        .build())
                                .build());

                toolResultFutures.add(resultFuture);
            }
        }

        // Wait for all tool executions, then send results back
        return CompletableFuture.allOf(toolResultFutures.toArray(new CompletableFuture[0]))
                .thenCompose(v -> {
                    List<ContentBlock> resultBlocks = toolResultFutures.stream()
                            .map(CompletableFuture::join)
                            .map(ContentBlock::fromToolResult)
                            .toList();

                    messages.add(Message.builder()
                            .role(ConversationRole.USER)
                            .content(resultBlocks)
                            .build());

                    return converseLoop(messages, toolCalls, iteration + 1, maxIterations,
                            temperature, startTime);
                });
    }

    private CompletableFuture<String> executeTool(String toolName, software.amazon.awssdk.core.document.Document input) {
        return switch (toolName) {
            case "searchFlights" -> {
                String origin = input.asMap().get("origin").asString();
                String destination = input.asMap().get("destination").asString();
                String date = input.asMap().get("date").asString();
                yield flightTool.searchFlights(origin, destination, date)
                        .thenApply(this::toJson);
            }
            case "getFlightDetails" -> {
                String flightId = input.asMap().get("flightId").asString();
                yield flightTool.getFlightDetails(flightId)
                        .thenApply(this::toJson);
            }
            default -> CompletableFuture.failedFuture(
                    new AgentException(AgentException.ErrorCode.TOOL_NOT_FOUND,
                            "Unknown tool: " + toolName));
        };
    }

    private String extractText(ConverseResponse response) {
        if (response.output() == null || response.output().message() == null) {
            return "";
        }
        return response.output().message().content().stream()
                .filter(block -> block.text() != null)
                .map(ContentBlock::text)
                .findFirst()
                .orElse("");
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.error("Failed to serialize tool result to JSON", e);
            return "[]";
        }
    }
}
