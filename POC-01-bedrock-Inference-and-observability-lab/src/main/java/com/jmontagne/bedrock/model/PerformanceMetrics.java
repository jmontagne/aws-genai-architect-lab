package com.jmontagne.bedrock.model;

import java.time.Duration;

public record PerformanceMetrics(
        long timeToFirstTokenMs,
        long totalGenerationTimeMs,
        int inputTokens,
        int outputTokens,
        ModelType modelType
) {
    public double tokensPerSecond() {
        if (totalGenerationTimeMs == 0) return 0;
        return (outputTokens * 1000.0) / totalGenerationTimeMs;
    }

    public Duration timeToFirstToken() {
        return Duration.ofMillis(timeToFirstTokenMs);
    }

    public Duration totalGenerationTime() {
        return Duration.ofMillis(totalGenerationTimeMs);
    }

    public int totalTokens() {
        return inputTokens + outputTokens;
    }

    @Override
    public String toString() {
        return String.format(
                "PerformanceMetrics[model=%s, TTFT=%dms, total=%dms, tokens=%d/%d, %.2f tok/s]",
                modelType.getDisplayName(),
                timeToFirstTokenMs,
                totalGenerationTimeMs,
                inputTokens,
                outputTokens,
                tokensPerSecond()
        );
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private long timeToFirstTokenMs;
        private long totalGenerationTimeMs;
        private int inputTokens;
        private int outputTokens;
        private ModelType modelType;

        public Builder timeToFirstTokenMs(long ttft) {
            this.timeToFirstTokenMs = ttft;
            return this;
        }

        public Builder totalGenerationTimeMs(long total) {
            this.totalGenerationTimeMs = total;
            return this;
        }

        public Builder inputTokens(int tokens) {
            this.inputTokens = tokens;
            return this;
        }

        public Builder outputTokens(int tokens) {
            this.outputTokens = tokens;
            return this;
        }

        public Builder modelType(ModelType type) {
            this.modelType = type;
            return this;
        }

        public PerformanceMetrics build() {
            return new PerformanceMetrics(
                    timeToFirstTokenMs,
                    totalGenerationTimeMs,
                    inputTokens,
                    outputTokens,
                    modelType
            );
        }
    }
}
