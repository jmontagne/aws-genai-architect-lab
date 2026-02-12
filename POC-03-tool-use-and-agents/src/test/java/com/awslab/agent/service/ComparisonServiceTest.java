package com.awslab.agent.service;

import com.awslab.agent.config.AgentProperties;
import com.awslab.agent.exception.AgentException;
import com.awslab.agent.model.AgentResponse;
import com.awslab.agent.model.ComparisonResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ComparisonServiceTest {

    @Mock
    private ToolUseService toolUseService;

    @Mock
    private AgentService agentService;

    @Mock
    private AgentProperties properties;

    @InjectMocks
    private ComparisonService comparisonService;

    @Test
    void compare_bothSucceed_returnsComparison() {
        when(properties.getMaxIterations()).thenReturn(5);
        when(properties.getTemperature()).thenReturn(0.0f);

        AgentResponse patternA = new AgentResponse("Answer from A", 2,
                List.of(new AgentResponse.ToolCall("searchFlights", "{}")), 3000L, null);
        AgentResponse patternB = new AgentResponse("Answer from B", 0,
                List.of(), 5000L, "cmp-123");

        when(toolUseService.execute(eq("Find flights"), eq(5), eq(0.0f)))
                .thenReturn(CompletableFuture.completedFuture(patternA));
        when(agentService.invoke(eq("Find flights"), anyString()))
                .thenReturn(CompletableFuture.completedFuture(patternB));

        ComparisonResponse result = comparisonService.compare("Find flights").join();

        assertThat(result.query()).isEqualTo("Find flights");
        assertThat(result.patternA().answer()).isEqualTo("Answer from A");
        assertThat(result.patternB().answer()).isEqualTo("Answer from B");
        assertThat(result.analysis().latencyDifferenceMs()).isEqualTo(2000L);
        assertThat(result.analysis().patternAIterations()).isEqualTo(2);
        assertThat(result.analysis().patternAToolCalls()).isEqualTo(1);
    }

    @Test
    void compare_patternAFails_propagatesException() {
        when(properties.getMaxIterations()).thenReturn(5);
        when(properties.getTemperature()).thenReturn(0.0f);

        when(toolUseService.execute(anyString(), anyInt(), anyFloat()))
                .thenReturn(CompletableFuture.failedFuture(
                        new AgentException(AgentException.ErrorCode.CONVERSE_API_FAILED, "API failed")));
        when(agentService.invoke(anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(
                        new AgentResponse("Answer B", 0, List.of(), 5000L, "session")));

        assertThatThrownBy(() -> comparisonService.compare("Find flights").join())
                .isInstanceOf(CompletionException.class);
    }

    @Test
    void compare_patternBFails_propagatesException() {
        when(properties.getMaxIterations()).thenReturn(5);
        when(properties.getTemperature()).thenReturn(0.0f);

        when(toolUseService.execute(anyString(), anyInt(), anyFloat()))
                .thenReturn(CompletableFuture.completedFuture(
                        new AgentResponse("Answer A", 2, List.of(), 3000L, null)));
        when(agentService.invoke(anyString(), anyString()))
                .thenReturn(CompletableFuture.failedFuture(
                        new AgentException(AgentException.ErrorCode.AGENT_INVOCATION_FAILED, "Agent failed")));

        assertThatThrownBy(() -> comparisonService.compare("Find flights").join())
                .isInstanceOf(CompletionException.class);
    }
}
