package com.jmontagne.bedrock.client;

import com.jmontagne.bedrock.model.InferenceParameters;
import com.jmontagne.bedrock.model.InferenceRequest;
import com.jmontagne.bedrock.model.ModelType;
import com.jmontagne.bedrock.model.PerformanceMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeAsyncClient;
import software.amazon.awssdk.services.bedrockruntime.model.*;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Reactive bridge between the Bedrock Converse Stream API and Project Reactor.
 *
 * <p>Converts the AWS SDK's callback-based {@link ConverseStreamResponseHandler} into a
 * reactive {@code Flux<String>} using {@link Sinks.Many}. This enables backpressure-aware
 * streaming and composability with Spring WebFlux.</p>
 *
 * <h3>TTFT Measurement</h3>
 * <p>Tracks <b>Time-To-First-Token (TTFT)</b> independently of total generation time.
 * The first {@link ContentBlockDeltaEvent} triggers a timestamp capture, giving an
 * accurate measure of model inference latency without including network or generation time.</p>
 *
 * <h3>Token Usage Tracking</h3>
 * <p>Extracts input/output token counts from {@link ConverseStreamMetadataEvent} for
 * cost calculation and observability. Metrics are captured via {@link PerformanceMetrics}
 * and logged on stream completion.</p>
 *
 * @see InferenceService High-level orchestration layer
 */
@Component
public class BedrockStreamingClient {

    private static final Logger log = LoggerFactory.getLogger(BedrockStreamingClient.class);

    private final BedrockRuntimeAsyncClient bedrockClient;

    public BedrockStreamingClient(BedrockRuntimeAsyncClient bedrockClient) {
        this.bedrockClient = bedrockClient;
    }

    public Flux<String> streamConverse(InferenceRequest request) {
        Sinks.Many<String> sink = Sinks.many().unicast().onBackpressureBuffer();

        long startTime = System.currentTimeMillis();
        AtomicBoolean firstTokenReceived = new AtomicBoolean(false);
        AtomicLong ttftMs = new AtomicLong(0);
        AtomicInteger inputTokens = new AtomicInteger(0);
        AtomicInteger outputTokens = new AtomicInteger(0);

        ConverseStreamRequest converseRequest = buildConverseRequest(request);

        log.info("Starting streaming inference with model: {}", request.modelType().getDisplayName());

        ConverseStreamResponseHandler handler = ConverseStreamResponseHandler.builder()
                .onEventStream(publisher -> publisher.subscribe(event -> {
                    handleStreamEvent(event, sink, startTime, firstTokenReceived, ttftMs, inputTokens, outputTokens, request.modelType());
                }))
                .onError(error -> {
                    log.error("Streaming error for model {}: {}", request.modelType().getDisplayName(), error.getMessage());
                    sink.tryEmitError(error);
                })
                .onComplete(() -> {
                    long totalTime = System.currentTimeMillis() - startTime;
                    PerformanceMetrics metrics = PerformanceMetrics.builder()
                            .timeToFirstTokenMs(ttftMs.get())
                            .totalGenerationTimeMs(totalTime)
                            .inputTokens(inputTokens.get())
                            .outputTokens(outputTokens.get())
                            .modelType(request.modelType())
                            .build();

                    log.info("Streaming completed: {}", metrics);
                    sink.tryEmitComplete();
                })
                .build();

        bedrockClient.converseStream(converseRequest, handler)
                .exceptionally(throwable -> {
                    log.error("Failed to initiate streaming: {}", throwable.getMessage());
                    sink.tryEmitError(throwable);
                    return null;
                });

        return sink.asFlux();
    }

    private void handleStreamEvent(
            ConverseStreamOutput event,
            Sinks.Many<String> sink,
            long startTime,
            AtomicBoolean firstTokenReceived,
            AtomicLong ttftMs,
            AtomicInteger inputTokens,
            AtomicInteger outputTokens,
            ModelType modelType
    ) {
        if (event instanceof ContentBlockDeltaEvent deltaEvent) {
            if (!firstTokenReceived.getAndSet(true)) {
                ttftMs.set(System.currentTimeMillis() - startTime);
                log.debug("Time to first token: {}ms for model {}", ttftMs.get(), modelType.getDisplayName());
            }

            ContentBlockDelta delta = deltaEvent.delta();
            if (delta != null && delta.text() != null) {
                sink.tryEmitNext(delta.text());
            }
        } else if (event instanceof MessageStopEvent stopEvent) {
            log.debug("Message stop received with reason: {}", stopEvent.stopReason());
        } else if (event instanceof ConverseStreamMetadataEvent metadataEvent) {
            TokenUsage usage = metadataEvent.usage();
            if (usage != null) {
                inputTokens.set(usage.inputTokens());
                outputTokens.set(usage.outputTokens());
                log.debug("Token usage - Input: {}, Output: {}", usage.inputTokens(), usage.outputTokens());
            }
        }
    }

    private ConverseStreamRequest buildConverseRequest(InferenceRequest request) {
        InferenceParameters params = request.parameters();

        Message userMessage = Message.builder()
                .role(ConversationRole.USER)
                .content(ContentBlock.fromText(request.userMessage()))
                .build();

        SystemContentBlock systemBlock = SystemContentBlock.builder()
                .text(request.systemPrompt())
                .build();

        InferenceConfiguration.Builder inferenceConfig = InferenceConfiguration.builder()
                .maxTokens(params.maxTokens())
                .temperature(params.temperature().floatValue())
                .topP(params.topP().floatValue());

        if (params.stopSequences() != null && !params.stopSequences().isEmpty()) {
            inferenceConfig.stopSequences(params.stopSequences());
        }

        return ConverseStreamRequest.builder()
                .modelId(request.modelType().getModelId())
                .messages(List.of(userMessage))
                .system(List.of(systemBlock))
                .inferenceConfig(inferenceConfig.build())
                .build();
    }

    public PerformanceMetrics getLastMetrics() {
        return null;
    }
}
