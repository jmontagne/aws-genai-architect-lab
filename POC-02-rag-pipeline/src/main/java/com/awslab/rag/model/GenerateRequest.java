package com.awslab.rag.model;

import jakarta.validation.constraints.NotBlank;

public record GenerateRequest(
        @NotBlank(message = "Query is required")
        String query,

        Integer numberOfResults,

        Float temperature,

        Integer maxTokens
) {
    public GenerateRequest {
        if (numberOfResults == null) {
            numberOfResults = 5;
        }
        if (temperature == null) {
            temperature = 0.0f;
        }
        if (maxTokens == null) {
            maxTokens = 1024;
        }
    }
}
