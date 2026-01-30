package com.awslab.rag.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record EvaluationRequest(
        @NotBlank(message = "Query is required")
        String query,

        String answer,

        @NotEmpty(message = "At least one chunk is required")
        List<String> retrievedChunks
) {}
