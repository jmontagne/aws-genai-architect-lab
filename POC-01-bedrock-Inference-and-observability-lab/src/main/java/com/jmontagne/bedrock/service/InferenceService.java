package com.jmontagne.bedrock.service;

import com.jmontagne.bedrock.client.BedrockStreamingClient;
import com.jmontagne.bedrock.model.InferenceParameters;
import com.jmontagne.bedrock.model.InferenceRequest;
import com.jmontagne.bedrock.model.InferenceResponse;
import com.jmontagne.bedrock.model.ModelType;
import com.jmontagne.bedrock.model.PerformanceMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Orchestrates Bedrock inference with streaming and model comparison capabilities.
 *
 * <p>Provides two inference modes via the Bedrock Converse API:</p>
 * <ul>
 *   <li><b>Streaming ({@link #streamWithJacquesMontagne}):</b> Returns a reactive {@code Flux<String>}
 *       of token chunks — enables Time-To-First-Token (TTFT) measurement independently of
 *       total generation time.</li>
 *   <li><b>Non-streaming ({@link #inferWithJacquesMontagne}):</b> Collects the full response
 *       with {@link com.jmontagne.bedrock.model.PerformanceMetrics} (TTFT, latency, token counts).</li>
 * </ul>
 *
 * <h3>Model Comparison</h3>
 * <p>{@link #compareModels} runs Claude 3.5 Sonnet and Claude 3 Haiku in parallel
 * ({@code Mono.zip}) and returns side-by-side results with latency metrics —
 * demonstrating the cost vs. quality trade-off (Sonnet: $3/$15 vs. Haiku: $0.25/$1.25
 * per M tokens).</p>
 *
 * @see BedrockStreamingClient Low-level streaming bridge (Converse Stream API + Reactor Sinks)
 * @see com.jmontagne.bedrock.model.PerformanceMetrics TTFT, token usage, and latency metrics
 */
@Service
public class InferenceService {

    private static final Logger log = LoggerFactory.getLogger(InferenceService.class);

    private final BedrockStreamingClient streamingClient;

    public InferenceService(BedrockStreamingClient streamingClient) {
        this.streamingClient = streamingClient;
    }

    public Flux<String> streamWithJacquesMontagne(String userMessage, ModelType modelType) {
        return streamWithJacquesMontagne(userMessage, modelType, InferenceParameters.DEFAULT);
    }

    public Flux<String> streamWithJacquesMontagne(String userMessage, ModelType modelType, InferenceParameters parameters) {
        log.info("Processing request with Jacques Montagne persona - Model: {}, Temperature: {}",
                modelType.getDisplayName(), parameters.temperature());

        InferenceRequest request = InferenceRequest.withJacquesMontagne(userMessage, modelType, parameters);
        return streamingClient.streamConverse(request);
    }

    public Flux<String> streamCustom(String systemPrompt, String userMessage, ModelType modelType, InferenceParameters parameters) {
        log.info("Processing custom request - Model: {}", modelType.getDisplayName());

        InferenceRequest request = new InferenceRequest(systemPrompt, userMessage, modelType, parameters);
        return streamingClient.streamConverse(request);
    }

    public Mono<InferenceResponse> inferWithJacquesMontagne(String userMessage, ModelType modelType) {
        return inferWithJacquesMontagne(userMessage, modelType, InferenceParameters.DEFAULT);
    }

    public Mono<InferenceResponse> inferWithJacquesMontagne(String userMessage, ModelType modelType, InferenceParameters parameters) {
        log.info("Processing non-streaming request with Jacques Montagne - Model: {}", modelType.getDisplayName());

        long startTime = System.currentTimeMillis();

        return streamWithJacquesMontagne(userMessage, modelType, parameters)
                .collectList()
                .map(chunks -> {
                    String fullResponse = String.join("", chunks);
                    long totalTime = System.currentTimeMillis() - startTime;

                    PerformanceMetrics metrics = PerformanceMetrics.builder()
                            .totalGenerationTimeMs(totalTime)
                            .modelType(modelType)
                            .build();

                    return InferenceResponse.of(fullResponse, modelType, metrics);
                });
    }

    public Mono<String> compareModels(String userMessage, InferenceParameters parameters) {
        log.info("Comparing models with message: {}", userMessage.substring(0, Math.min(50, userMessage.length())));

        Mono<String> claudeResponse = inferWithJacquesMontagne(userMessage, ModelType.CLAUDE_3_5_SONNET, parameters)
                .map(response -> formatModelResponse("Claude 3.5 Sonnet", response));

        Mono<String> haikuResponse = inferWithJacquesMontagne(userMessage, ModelType.CLAUDE_3_HAIKU, parameters)
                .map(response -> formatModelResponse("Claude 3 Haiku", response));

        return Mono.zip(claudeResponse, haikuResponse)
                .map(tuple -> String.format("""
                        === Model Comparison Results ===

                        %s

                        ---

                        %s
                        """, tuple.getT1(), tuple.getT2()));
    }

    private String formatModelResponse(String modelName, InferenceResponse response) {
        return String.format("""
                [%s]
                Response: %s

                Metrics: %s
                """,
                modelName,
                response.content(),
                response.metrics()
        );
    }
}
