package com.awslab.agent.service;

import com.awslab.agent.config.AgentProperties;
import com.awslab.agent.exception.AgentException;
import com.awslab.agent.model.AgentResponse;
import com.awslab.agent.model.Flight;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.document.Document;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeAsyncClient;
import software.amazon.awssdk.services.bedrockruntime.model.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ToolUseServiceTest {

    @Mock
    private BedrockRuntimeAsyncClient bedrockClient;

    @Mock
    private FlightTool flightTool;

    @Mock
    private AgentProperties properties;

    private ToolUseService toolUseService;

    @BeforeEach
    void setUp() {
        toolUseService = new ToolUseService(bedrockClient, flightTool, properties,
                new ObjectMapper());
    }

    @Test
    void execute_directAnswer_returnsResponse() {
        when(properties.getModelId()).thenReturn("anthropic.claude-3-5-sonnet-20241022-v2:0");
        when(properties.getMaxTokens()).thenReturn(1024);

        ConverseResponse response = ConverseResponse.builder()
                .stopReason(StopReason.END_TURN)
                .output(ConverseOutput.builder()
                        .message(Message.builder()
                                .role(ConversationRole.ASSISTANT)
                                .content(ContentBlock.fromText("Hello, how can I help?"))
                                .build())
                        .build())
                .build();

        when(bedrockClient.converse(any(ConverseRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(response));

        AgentResponse result = toolUseService.execute("Hello", 5, 0.0f).join();

        assertThat(result.answer()).isEqualTo("Hello, how can I help?");
        assertThat(result.iterations()).isEqualTo(1);
        assertThat(result.toolCalls()).isEmpty();
    }

    @Test
    void execute_singleToolCall_returnsResponseWithToolCalls() {
        when(properties.getModelId()).thenReturn("anthropic.claude-3-5-sonnet-20241022-v2:0");
        when(properties.getMaxTokens()).thenReturn(1024);

        // First call: model requests tool use
        ConverseResponse toolUseResponse = ConverseResponse.builder()
                .stopReason(StopReason.TOOL_USE)
                .output(ConverseOutput.builder()
                        .message(Message.builder()
                                .role(ConversationRole.ASSISTANT)
                                .content(ContentBlock.fromToolUse(ToolUseBlock.builder()
                                        .toolUseId("tool-1")
                                        .name("searchFlights")
                                        .input(Document.mapBuilder()
                                                .putString("origin", "WAW")
                                                .putString("destination", "CDG")
                                                .putString("date", "2025-03-15")
                                                .build())
                                        .build()))
                                .build())
                        .build())
                .build();

        // Second call: model returns final answer
        ConverseResponse endTurnResponse = ConverseResponse.builder()
                .stopReason(StopReason.END_TURN)
                .output(ConverseOutput.builder()
                        .message(Message.builder()
                                .role(ConversationRole.ASSISTANT)
                                .content(ContentBlock.fromText("Found 1 flight from WAW to CDG."))
                                .build())
                        .build())
                .build();

        when(bedrockClient.converse(any(ConverseRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(toolUseResponse))
                .thenReturn(CompletableFuture.completedFuture(endTurnResponse));

        Flight flight = new Flight("WAW#CDG", "LO335", "2025-03-15", "LOT", "06:45", "09:10",
                "Boeing 737", 450.0, 42, true);
        when(flightTool.searchFlights("WAW", "CDG", "2025-03-15"))
                .thenReturn(CompletableFuture.completedFuture(List.of(flight)));

        AgentResponse result = toolUseService.execute("Find flights WAW to CDG", 5, 0.0f).join();

        assertThat(result.answer()).isEqualTo("Found 1 flight from WAW to CDG.");
        assertThat(result.iterations()).isEqualTo(2);
        assertThat(result.toolCalls()).hasSize(1);
        assertThat(result.toolCalls().get(0).tool()).isEqualTo("searchFlights");
    }

    @Test
    void execute_multiIterationChain_handlesCorrectly() {
        when(properties.getModelId()).thenReturn("anthropic.claude-3-5-sonnet-20241022-v2:0");
        when(properties.getMaxTokens()).thenReturn(1024);

        // Iteration 1: searchFlights
        ConverseResponse searchResponse = ConverseResponse.builder()
                .stopReason(StopReason.TOOL_USE)
                .output(ConverseOutput.builder()
                        .message(Message.builder()
                                .role(ConversationRole.ASSISTANT)
                                .content(ContentBlock.fromToolUse(ToolUseBlock.builder()
                                        .toolUseId("tool-1")
                                        .name("searchFlights")
                                        .input(Document.mapBuilder()
                                                .putString("origin", "WAW")
                                                .putString("destination", "CDG")
                                                .putString("date", "2025-03-15")
                                                .build())
                                        .build()))
                                .build())
                        .build())
                .build();

        // Iteration 2: getFlightDetails
        ConverseResponse detailsResponse = ConverseResponse.builder()
                .stopReason(StopReason.TOOL_USE)
                .output(ConverseOutput.builder()
                        .message(Message.builder()
                                .role(ConversationRole.ASSISTANT)
                                .content(ContentBlock.fromToolUse(ToolUseBlock.builder()
                                        .toolUseId("tool-2")
                                        .name("getFlightDetails")
                                        .input(Document.mapBuilder()
                                                .putString("flightId", "AF1145")
                                                .build())
                                        .build()))
                                .build())
                        .build())
                .build();

        // Iteration 3: final answer
        ConverseResponse finalResponse = ConverseResponse.builder()
                .stopReason(StopReason.END_TURN)
                .output(ConverseOutput.builder()
                        .message(Message.builder()
                                .role(ConversationRole.ASSISTANT)
                                .content(ContentBlock.fromText("The cheapest flight is AF1145 at 380 EUR."))
                                .build())
                        .build())
                .build();

        when(bedrockClient.converse(any(ConverseRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(searchResponse))
                .thenReturn(CompletableFuture.completedFuture(detailsResponse))
                .thenReturn(CompletableFuture.completedFuture(finalResponse));

        Flight flight = new Flight("WAW#CDG", "AF1145", "2025-03-15", "Air France", "08:30", "10:45",
                "Airbus A320", 380.0, 23, true);
        when(flightTool.searchFlights("WAW", "CDG", "2025-03-15"))
                .thenReturn(CompletableFuture.completedFuture(List.of(flight)));
        when(flightTool.getFlightDetails("AF1145"))
                .thenReturn(CompletableFuture.completedFuture(List.of(flight)));

        AgentResponse result = toolUseService.execute("Find cheapest flight", 5, 0.0f).join();

        assertThat(result.answer()).isEqualTo("The cheapest flight is AF1145 at 380 EUR.");
        assertThat(result.iterations()).isEqualTo(3);
        assertThat(result.toolCalls()).hasSize(2);
        assertThat(result.toolCalls().get(0).tool()).isEqualTo("searchFlights");
        assertThat(result.toolCalls().get(1).tool()).isEqualTo("getFlightDetails");
    }

    @Test
    void execute_maxIterationsExceeded_throwsException() {
        when(properties.getModelId()).thenReturn("anthropic.claude-3-5-sonnet-20241022-v2:0");
        when(properties.getMaxTokens()).thenReturn(1024);

        ConverseResponse toolUseResponse = ConverseResponse.builder()
                .stopReason(StopReason.TOOL_USE)
                .output(ConverseOutput.builder()
                        .message(Message.builder()
                                .role(ConversationRole.ASSISTANT)
                                .content(ContentBlock.fromToolUse(ToolUseBlock.builder()
                                        .toolUseId("tool-1")
                                        .name("searchFlights")
                                        .input(Document.mapBuilder()
                                                .putString("origin", "WAW")
                                                .putString("destination", "CDG")
                                                .putString("date", "2025-03-15")
                                                .build())
                                        .build()))
                                .build())
                        .build())
                .build();

        when(bedrockClient.converse(any(ConverseRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(toolUseResponse));

        Flight flight = new Flight("WAW#CDG", "LO335", "2025-03-15", "LOT", "06:45", "09:10",
                "Boeing 737", 450.0, 42, true);
        when(flightTool.searchFlights("WAW", "CDG", "2025-03-15"))
                .thenReturn(CompletableFuture.completedFuture(List.of(flight)));

        assertThatThrownBy(() -> toolUseService.execute("Find flights", 1, 0.0f).join())
                .isInstanceOf(CompletionException.class)
                .hasCauseInstanceOf(AgentException.class)
                .hasRootCauseMessage("Exceeded maximum iterations: 1");
    }

    @Test
    void execute_unknownTool_throwsException() {
        when(properties.getModelId()).thenReturn("anthropic.claude-3-5-sonnet-20241022-v2:0");
        when(properties.getMaxTokens()).thenReturn(1024);

        ConverseResponse response = ConverseResponse.builder()
                .stopReason(StopReason.TOOL_USE)
                .output(ConverseOutput.builder()
                        .message(Message.builder()
                                .role(ConversationRole.ASSISTANT)
                                .content(ContentBlock.fromToolUse(ToolUseBlock.builder()
                                        .toolUseId("tool-1")
                                        .name("unknownTool")
                                        .input(Document.mapBuilder()
                                                .putString("param", "value")
                                                .build())
                                        .build()))
                                .build())
                        .build())
                .build();

        when(bedrockClient.converse(any(ConverseRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(response));

        assertThatThrownBy(() -> toolUseService.execute("Do something", 5, 0.0f).join())
                .isInstanceOf(CompletionException.class)
                .hasCauseInstanceOf(AgentException.class);
    }

    @Test
    void execute_apiFailure_throwsException() {
        when(properties.getModelId()).thenReturn("anthropic.claude-3-5-sonnet-20241022-v2:0");
        when(properties.getMaxTokens()).thenReturn(1024);

        when(bedrockClient.converse(any(ConverseRequest.class)))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("API error")));

        assertThatThrownBy(() -> toolUseService.execute("Hello", 5, 0.0f).join())
                .isInstanceOf(CompletionException.class)
                .hasCauseInstanceOf(AgentException.class);
    }
}
