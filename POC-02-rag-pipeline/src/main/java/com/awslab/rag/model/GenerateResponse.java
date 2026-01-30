package com.awslab.rag.model;

import java.util.List;

public record GenerateResponse(
        String query,
        String answer,
        List<Citation> citations,
        long latencyMs
) {}
