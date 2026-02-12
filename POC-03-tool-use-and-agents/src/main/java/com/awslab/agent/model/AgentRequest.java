package com.awslab.agent.model;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record AgentRequest(
        @NotBlank(message = "Message is required")
        String message,

        String sessionId
) {
    public AgentRequest {
        if (sessionId == null || sessionId.isBlank()) {
            sessionId = UUID.randomUUID().toString();
        }
    }
}
