package com.jmontagne.bedrock.model;

import java.util.List;

public record InferenceParameters(
        Double temperature,
        Double topP,
        Integer maxTokens,
        List<String> stopSequences
) {
    public static final InferenceParameters DEFAULT = new InferenceParameters(
            0.7,
            0.9,
            2048,
            List.of()
    );

    public static InferenceParameters withTemperature(double temperature) {
        return new InferenceParameters(temperature, DEFAULT.topP(), DEFAULT.maxTokens(), DEFAULT.stopSequences());
    }

    public static InferenceParameters deterministic() {
        return new InferenceParameters(0.0, 1.0, 2048, List.of());
    }

    public InferenceParameters withMaxTokens(int maxTokens) {
        return new InferenceParameters(this.temperature, this.topP, maxTokens, this.stopSequences);
    }

    public InferenceParameters withStopSequences(List<String> stopSequences) {
        return new InferenceParameters(this.temperature, this.topP, this.maxTokens, stopSequences);
    }
}
