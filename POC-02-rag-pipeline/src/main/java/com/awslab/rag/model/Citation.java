package com.awslab.rag.model;

public record Citation(
        String text,
        String sourceUri,
        String score,
        GeneratedSpan generatedSpan
) {
    public record GeneratedSpan(
            int start,
            int end
    ) {}

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String text;
        private String sourceUri;
        private String score;
        private GeneratedSpan generatedSpan;

        public Builder text(String text) {
            this.text = text;
            return this;
        }

        public Builder sourceUri(String sourceUri) {
            this.sourceUri = sourceUri;
            return this;
        }

        public Builder score(String score) {
            this.score = score;
            return this;
        }

        public Builder generatedSpan(int start, int end) {
            this.generatedSpan = new GeneratedSpan(start, end);
            return this;
        }

        public Citation build() {
            return new Citation(text, sourceUri, score, generatedSpan);
        }
    }
}
