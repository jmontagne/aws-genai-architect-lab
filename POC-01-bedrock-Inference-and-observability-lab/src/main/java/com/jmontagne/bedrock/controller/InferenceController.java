package com.jmontagne.bedrock.controller;

import com.jmontagne.bedrock.model.InferenceParameters;
import com.jmontagne.bedrock.model.InferenceResponse;
import com.jmontagne.bedrock.model.ModelType;
import com.jmontagne.bedrock.service.InferenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

/**
 * REST controller for Bedrock inference operations.
 *
 * Uses blocking calls (.block()) for Lambda servlet adapter compatibility.
 * The aws-serverless-java-container-springboot3 library doesn't properly
 * handle reactive types (Mono/Flux), so we convert to synchronous responses.
 */
@RestController
@RequestMapping("/api/v1/inference")
public class InferenceController {

    private static final Logger log = LoggerFactory.getLogger(InferenceController.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(60);

    private final InferenceService inferenceService;

    public InferenceController(InferenceService inferenceService) {
        this.inferenceService = inferenceService;
    }

    /**
     * Streaming inference endpoint - collects all chunks and returns complete response.
     * Note: True SSE streaming is not supported through API Gateway + Lambda without
     * Lambda Response Streaming. This endpoint buffers the full response.
     */
    @GetMapping(value = "/stream/{modelType}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<StreamResponse> streamInference(
            @PathVariable ModelType modelType,
            @RequestParam String message,
            @RequestParam(defaultValue = "0.7") Double temperature,
            @RequestParam(defaultValue = "0.9") Double topP,
            @RequestParam(defaultValue = "2048") Integer maxTokens
    ) {
        log.info("Streaming inference request - Model: {}, Message: {}",
                modelType.getDisplayName(),
                message.substring(0, Math.min(50, message.length())));

        InferenceParameters parameters = new InferenceParameters(temperature, topP, maxTokens, List.of());

        List<String> chunks = inferenceService.streamWithJacquesMontagne(message, modelType, parameters)
                .collectList()
                .block(TIMEOUT);

        String fullResponse = chunks != null ? String.join("", chunks) : "";

        return ResponseEntity.ok(new StreamResponse(
                fullResponse,
                modelType.name(),
                modelType.getDisplayName(),
                chunks != null ? chunks.size() : 0
        ));
    }

    /**
     * Streaming inference via POST with custom system prompt support.
     */
    @PostMapping(value = "/stream/{modelType}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<StreamResponse> streamInferencePost(
            @PathVariable ModelType modelType,
            @RequestBody InferenceRequestBody body
    ) {
        log.info("Streaming inference POST request - Model: {}", modelType.getDisplayName());

        InferenceParameters parameters = new InferenceParameters(
                body.temperature() != null ? body.temperature() : 0.7,
                body.topP() != null ? body.topP() : 0.9,
                body.maxTokens() != null ? body.maxTokens() : 2048,
                body.stopSequences() != null ? body.stopSequences() : List.of()
        );

        List<String> chunks;
        if (body.systemPrompt() != null) {
            chunks = inferenceService.streamCustom(body.systemPrompt(), body.message(), modelType, parameters)
                    .collectList()
                    .block(TIMEOUT);
        } else {
            chunks = inferenceService.streamWithJacquesMontagne(body.message(), modelType, parameters)
                    .collectList()
                    .block(TIMEOUT);
        }

        String fullResponse = chunks != null ? String.join("", chunks) : "";

        return ResponseEntity.ok(new StreamResponse(
                fullResponse,
                modelType.name(),
                modelType.getDisplayName(),
                chunks != null ? chunks.size() : 0
        ));
    }

    /**
     * Non-streaming inference - returns complete response with metrics.
     */
    @GetMapping("/{modelType}")
    public ResponseEntity<InferenceResponse> inference(
            @PathVariable ModelType modelType,
            @RequestParam String message,
            @RequestParam(defaultValue = "0.7") Double temperature
    ) {
        log.info("Non-streaming inference request - Model: {}", modelType.getDisplayName());

        InferenceParameters parameters = InferenceParameters.withTemperature(temperature);
        InferenceResponse response = inferenceService.inferWithJacquesMontagne(message, modelType, parameters)
                .block(TIMEOUT);

        return ResponseEntity.ok(response);
    }

    /**
     * Compare responses from multiple models.
     */
    @GetMapping("/compare")
    public ResponseEntity<String> compareModels(
            @RequestParam String message,
            @RequestParam(defaultValue = "0.0") Double temperature
    ) {
        log.info("Model comparison request");

        InferenceParameters parameters = InferenceParameters.withTemperature(temperature);
        String result = inferenceService.compareModels(message, parameters)
                .block(TIMEOUT);

        return ResponseEntity.ok(result);
    }

    /**
     * List available Bedrock models.
     */
    @GetMapping(value = "/models", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<ModelInfo> getAvailableModels() {
        log.info("Get available models endpoint called");
        return Arrays.stream(ModelType.values())
                .map(type -> new ModelInfo(
                        type.name(),
                        type.getModelId(),
                        type.getDisplayName(),
                        type.getProvider()
                ))
                .toList();
    }

    /**
     * Health check endpoint.
     */
    @GetMapping(value = "/health", produces = MediaType.APPLICATION_JSON_VALUE)
    public HealthResponse health() {
        log.info("Health check endpoint called");
        return new HealthResponse("OK", "bedrock-inference-lab");
    }

    public record HealthResponse(
            String status,
            String service
    ) {}

    public record InferenceRequestBody(
            String message,
            String systemPrompt,
            Double temperature,
            Double topP,
            Integer maxTokens,
            List<String> stopSequences
    ) {}

    public record ModelInfo(
            String enumName,
            String modelId,
            String displayName,
            String provider
    ) {}

    public record StreamResponse(
            String content,
            String model,
            String modelDisplayName,
            int chunksReceived
    ) {}
}
