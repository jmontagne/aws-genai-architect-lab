package com.awslab.rag.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.util.Map;

public record RetrievalRequest(
        @NotBlank(message = "Query is required")
        String query,

        @Min(value = 1, message = "Number of results must be at least 1")
        @Max(value = 100, message = "Number of results must not exceed 100")
        Integer numberOfResults,

        String searchType,

        Map<String, String> filter
) {
    public RetrievalRequest {
        if (numberOfResults == null) {
            numberOfResults = 5;
        }
        if (searchType == null) {
            searchType = "SEMANTIC";
        }
    }
}
