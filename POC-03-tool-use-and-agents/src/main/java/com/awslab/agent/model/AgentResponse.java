package com.awslab.agent.model;

import java.util.List;

public record AgentResponse(
        String answer,
        int iterations,
        List<ToolCall> toolCalls,
        long latencyMs,
        String sessionId
) {
    public record ToolCall(
            String tool,
            String input
    ) {}
}
