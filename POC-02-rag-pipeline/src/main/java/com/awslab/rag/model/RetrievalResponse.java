package com.awslab.rag.model;

import java.util.List;
import java.util.Map;

public record RetrievalResponse(
        String query,
        List<RetrievedChunk> chunks,
        int totalResults,
        long latencyMs
) {
    public record RetrievedChunk(
            String content,
            String sourceUri,
            Double score,
            Map<String, String> metadata
    ) {}
}
