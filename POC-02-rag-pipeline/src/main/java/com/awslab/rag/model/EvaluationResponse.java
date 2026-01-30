package com.awslab.rag.model;

public record EvaluationResponse(
        String query,
        Double relevanceScore,
        Double groundednessScore,
        Double faithfulnessScore,
        String explanation,
        long latencyMs
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String query;
        private Double relevanceScore;
        private Double groundednessScore;
        private Double faithfulnessScore;
        private String explanation;
        private long latencyMs;

        public Builder query(String query) {
            this.query = query;
            return this;
        }

        public Builder relevanceScore(Double relevanceScore) {
            this.relevanceScore = relevanceScore;
            return this;
        }

        public Builder groundednessScore(Double groundednessScore) {
            this.groundednessScore = groundednessScore;
            return this;
        }

        public Builder faithfulnessScore(Double faithfulnessScore) {
            this.faithfulnessScore = faithfulnessScore;
            return this;
        }

        public Builder explanation(String explanation) {
            this.explanation = explanation;
            return this;
        }

        public Builder latencyMs(long latencyMs) {
            this.latencyMs = latencyMs;
            return this;
        }

        public EvaluationResponse build() {
            return new EvaluationResponse(query, relevanceScore, groundednessScore,
                    faithfulnessScore, explanation, latencyMs);
        }
    }
}
