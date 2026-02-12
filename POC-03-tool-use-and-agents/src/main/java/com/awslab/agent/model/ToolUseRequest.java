package com.awslab.agent.model;

import jakarta.validation.constraints.NotBlank;

public record ToolUseRequest(
        @NotBlank(message = "Message is required")
        String message,

        Integer maxIterations,

        Float temperature
) {
    public ToolUseRequest {
        if (maxIterations == null) {
            maxIterations = 5;
        }
        if (temperature == null) {
            temperature = 0.0f;
        }
    }
}
