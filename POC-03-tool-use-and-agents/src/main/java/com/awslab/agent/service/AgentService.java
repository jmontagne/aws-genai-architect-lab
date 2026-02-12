package com.awslab.agent.service;

import com.awslab.agent.config.AgentProperties;
import com.awslab.agent.exception.AgentException;
import com.awslab.agent.model.AgentResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.bedrockagentruntime.BedrockAgentRuntimeAsyncClient;
import software.amazon.awssdk.services.bedrockagentruntime.model.InvokeAgentRequest;
import software.amazon.awssdk.services.bedrockagentruntime.model.InvokeAgentResponseHandler;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class AgentService {

    private static final Logger log = LoggerFactory.getLogger(AgentService.class);

    private final BedrockAgentRuntimeAsyncClient agentClient;
    private final AgentProperties properties;

    public AgentService(BedrockAgentRuntimeAsyncClient agentClient, AgentProperties properties) {
        this.agentClient = agentClient;
        this.properties = properties;
    }

    public CompletableFuture<AgentResponse> invoke(String message, String sessionId) {
        long startTime = System.currentTimeMillis();
        log.debug("Invoking Bedrock Agent: agentId={}, sessionId={}", properties.getAgentId(), sessionId);

        InvokeAgentRequest request = InvokeAgentRequest.builder()
                .agentId(properties.getAgentId())
                .agentAliasId(properties.getAgentAliasId())
                .sessionId(sessionId)
                .inputText(message)
                .build();

        StringBuilder responseText = new StringBuilder();

        InvokeAgentResponseHandler handler = InvokeAgentResponseHandler.builder()
                .subscriber(InvokeAgentResponseHandler.Visitor.builder()
                        .onChunk(chunk -> {
                            if (chunk.bytes() != null) {
                                String text = chunk.bytes().asString(StandardCharsets.UTF_8);
                                responseText.append(text);
                            }
                        })
                        .build())
                .onError(error -> log.error("Agent stream error", error))
                .build();

        return agentClient.invokeAgent(request, handler)
                .thenApply(response -> {
                    long latency = System.currentTimeMillis() - startTime;
                    String answer = responseText.toString();
                    log.debug("Agent response received in {}ms: {}...",
                            latency, answer.substring(0, Math.min(100, answer.length())));
                    return new AgentResponse(answer, 0, List.of(), latency, sessionId);
                })
                .exceptionally(ex -> {
                    log.error("Agent invocation failed for sessionId={}", sessionId, ex);
                    throw new AgentException(AgentException.ErrorCode.AGENT_INVOCATION_FAILED,
                            "Agent invocation failed: " + ex.getMessage(), ex);
                });
    }
}
