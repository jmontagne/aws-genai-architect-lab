package com.awslab.rag.service;

import com.awslab.rag.config.RagProperties;
import com.awslab.rag.exception.RagException;
import com.awslab.rag.model.EvaluationResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeAsyncClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EvaluationServiceTest {

    @Mock
    private BedrockRuntimeAsyncClient bedrockClient;

    @Mock
    private RagProperties ragProperties;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private EvaluationService evaluationService;

    @BeforeEach
    void setUp() throws Exception {
        evaluationService = new EvaluationService(bedrockClient, ragProperties, objectMapper);
        setField("evaluationEnabled", true);
    }

    private void setField(String name, Object value) throws Exception {
        Field field = EvaluationService.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(evaluationService, value);
    }

    private InvokeModelResponse buildClaudeResponse(String text) {
        String json = """
                {"content":[{"type":"text","text":"%s"}]}
                """.formatted(text);
        return InvokeModelResponse.builder()
                .body(SdkBytes.fromString(json, StandardCharsets.UTF_8))
                .build();
    }

    @Test
    void evaluate_disabled_returnsEarlyWithExplanation() throws Exception {
        setField("evaluationEnabled", false);

        EvaluationResponse result = evaluationService
                .evaluate("query", "answer", List.of("chunk")).join();

        assertThat(result.query()).isEqualTo("query");
        assertThat(result.explanation()).isEqualTo("Evaluation is disabled");
        verifyNoInteractions(bedrockClient);
    }

    @Test
    void evaluate_happyPath_computesBothScores() {
        when(ragProperties.getModelId()).thenReturn("anthropic.claude-3-sonnet-20240229-v1:0");

        when(bedrockClient.invokeModel(any(InvokeModelRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(buildClaudeResponse("7")))
                .thenReturn(CompletableFuture.completedFuture(buildClaudeResponse("8")));

        EvaluationResponse result = evaluationService
                .evaluate("query", "test answer", List.of("chunk1", "chunk2")).join();

        assertThat(result.query()).isEqualTo("query");
        assertThat(result.relevanceScore()).isBetween(0.0, 1.0);
        assertThat(result.groundednessScore()).isBetween(0.0, 1.0);
        assertThat(result.latencyMs()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void evaluate_nullAnswer_groundednessIsZero() {
        when(ragProperties.getModelId()).thenReturn("anthropic.claude-3-sonnet-20240229-v1:0");

        when(bedrockClient.invokeModel(any(InvokeModelRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(buildClaudeResponse("7")));

        EvaluationResponse result = evaluationService
                .evaluate("query", null, List.of("chunk1")).join();

        assertThat(result.groundednessScore()).isEqualTo(0.0);
        verify(bedrockClient, times(1)).invokeModel(any(InvokeModelRequest.class));
    }

    @Test
    void evaluate_emptyAnswer_groundednessIsZero() {
        when(ragProperties.getModelId()).thenReturn("anthropic.claude-3-sonnet-20240229-v1:0");

        when(bedrockClient.invokeModel(any(InvokeModelRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(buildClaudeResponse("7")));

        EvaluationResponse result = evaluationService
                .evaluate("query", "", List.of("chunk1")).join();

        assertThat(result.groundednessScore()).isEqualTo(0.0);
        verify(bedrockClient, times(1)).invokeModel(any(InvokeModelRequest.class));
    }

    @Test
    void evaluate_scoreNormalization_sevenBecomes0Point7() {
        when(ragProperties.getModelId()).thenReturn("anthropic.claude-3-sonnet-20240229-v1:0");

        when(bedrockClient.invokeModel(any(InvokeModelRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(buildClaudeResponse("7")));

        EvaluationResponse result = evaluationService
                .evaluate("query", null, List.of("chunk1")).join();

        assertThat(result.relevanceScore()).isEqualTo(0.7);
    }

    @Test
    void evaluate_nonNumericResponse_fallsBackTo0Point5() {
        when(ragProperties.getModelId()).thenReturn("anthropic.claude-3-sonnet-20240229-v1:0");

        when(bedrockClient.invokeModel(any(InvokeModelRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(buildClaudeResponse("not a number")));

        EvaluationResponse result = evaluationService
                .evaluate("query", null, List.of("chunk1")).join();

        assertThat(result.relevanceScore()).isEqualTo(0.5);
    }

    @Test
    void evaluate_correctModelIdPassedToRequest() {
        when(ragProperties.getModelId()).thenReturn("anthropic.claude-3-sonnet-20240229-v1:0");

        when(bedrockClient.invokeModel(any(InvokeModelRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(buildClaudeResponse("7")));

        evaluationService.evaluate("query", null, List.of("chunk")).join();

        ArgumentCaptor<InvokeModelRequest> captor = ArgumentCaptor.forClass(InvokeModelRequest.class);
        verify(bedrockClient).invokeModel(captor.capture());

        assertThat(captor.getValue().modelId()).isEqualTo("anthropic.claude-3-sonnet-20240229-v1:0");
    }

    @Test
    void evaluate_twoInvocationsWhenAnswerProvided() {
        when(ragProperties.getModelId()).thenReturn("anthropic.claude-3-sonnet-20240229-v1:0");

        when(bedrockClient.invokeModel(any(InvokeModelRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(buildClaudeResponse("7")));

        evaluationService.evaluate("query", "answer", List.of("chunk")).join();

        verify(bedrockClient, times(2)).invokeModel(any(InvokeModelRequest.class));
    }

    @Test
    void evaluate_oneInvocationWhenAnswerNull() {
        when(ragProperties.getModelId()).thenReturn("anthropic.claude-3-sonnet-20240229-v1:0");

        when(bedrockClient.invokeModel(any(InvokeModelRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(buildClaudeResponse("7")));

        evaluationService.evaluate("query", null, List.of("chunk")).join();

        verify(bedrockClient, times(1)).invokeModel(any(InvokeModelRequest.class));
    }

    @Test
    void evaluate_sdkException_throwsRagException() {
        when(ragProperties.getModelId()).thenReturn("anthropic.claude-3-sonnet-20240229-v1:0");

        when(bedrockClient.invokeModel(any(InvokeModelRequest.class)))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Bedrock error")));

        assertThatThrownBy(() -> evaluationService
                .evaluate("query", null, List.of("chunk")).join())
                .isInstanceOf(CompletionException.class)
                .hasCauseInstanceOf(RagException.class);
    }
}
