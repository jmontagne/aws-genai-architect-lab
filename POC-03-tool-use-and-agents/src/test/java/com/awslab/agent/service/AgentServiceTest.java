package com.awslab.agent.service;

import com.awslab.agent.config.AgentProperties;
import com.awslab.agent.exception.AgentException;
import com.awslab.agent.model.AgentResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.bedrockagentruntime.BedrockAgentRuntimeAsyncClient;
import software.amazon.awssdk.services.bedrockagentruntime.model.InvokeAgentRequest;
import software.amazon.awssdk.services.bedrockagentruntime.model.InvokeAgentResponseHandler;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentServiceTest {

    @Mock
    private BedrockAgentRuntimeAsyncClient agentClient;

    @Mock
    private AgentProperties properties;

    @InjectMocks
    private AgentService agentService;

    @Test
    void invoke_happyPath_returnsResponse() {
        when(properties.getAgentId()).thenReturn("agent-123");
        when(properties.getAgentAliasId()).thenReturn("alias-456");

        // Mock the invokeAgent call to complete immediately (empty response)
        when(agentClient.invokeAgent(any(InvokeAgentRequest.class), any(InvokeAgentResponseHandler.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        AgentResponse result = agentService.invoke("Find flights", "session-1").join();

        assertThat(result.sessionId()).isEqualTo("session-1");
        assertThat(result.latencyMs()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void invoke_sdkException_throwsAgentException() {
        when(properties.getAgentId()).thenReturn("agent-123");
        when(properties.getAgentAliasId()).thenReturn("alias-456");

        when(agentClient.invokeAgent(any(InvokeAgentRequest.class), any(InvokeAgentResponseHandler.class)))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("SDK error")));

        assertThatThrownBy(() -> agentService.invoke("Find flights", "session-1").join())
                .isInstanceOf(CompletionException.class)
                .hasCauseInstanceOf(AgentException.class);
    }

    @Test
    void invoke_emptyResponse_returnsEmptyAnswer() {
        when(properties.getAgentId()).thenReturn("agent-123");
        when(properties.getAgentAliasId()).thenReturn("alias-456");

        when(agentClient.invokeAgent(any(InvokeAgentRequest.class), any(InvokeAgentResponseHandler.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        AgentResponse result = agentService.invoke("Find flights", "session-1").join();

        assertThat(result.answer()).isEmpty();
        assertThat(result.toolCalls()).isEmpty();
        assertThat(result.iterations()).isEqualTo(0);
    }
}
