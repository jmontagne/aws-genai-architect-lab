package com.awslab.agent.model;

public record ComparisonResponse(
        String query,
        AgentResponse patternA,
        AgentResponse patternB,
        Analysis analysis
) {
    public record Analysis(
            long latencyDifferenceMs,
            int patternAIterations,
            int patternAToolCalls,
            long patternALatencyMs,
            long patternBLatencyMs
    ) {}
}
