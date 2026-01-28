package com.jmontagne.bedrock.model;

public record InferenceResponse(
        String content,
        ModelType modelType,
        PerformanceMetrics metrics,
        String finishReason
) {
    public static InferenceResponse of(String content, ModelType modelType, PerformanceMetrics metrics) {
        return new InferenceResponse(content, modelType, metrics, "end_turn");
    }

    public static InferenceResponse of(String content, ModelType modelType, PerformanceMetrics metrics, String finishReason) {
        return new InferenceResponse(content, modelType, metrics, finishReason);
    }
}
