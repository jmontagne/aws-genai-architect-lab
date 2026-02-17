package com.awslab.rag.service;

import com.awslab.rag.config.RagProperties;
import com.awslab.rag.exception.RagException;
import com.awslab.rag.model.EvaluationResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeAsyncClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * LLM-as-Judge evaluation framework for RAG quality assessment.
 *
 * <p>Scores RAG pipeline output on two axes using Claude 3 Haiku as the judge model:</p>
 * <ul>
 *   <li><b>Relevance (0.0–1.0):</b> Are the retrieved chunks relevant to the query?
 *       Measures retrieval quality independently of generation.</li>
 *   <li><b>Groundedness (0.0–1.0):</b> Is the generated answer supported by the retrieved
 *       chunks? Detects hallucination — claims not backed by source documents.</li>
 * </ul>
 *
 * <h3>Architecture</h3>
 * <p>Both scores are computed in parallel via {@code CompletableFuture.thenCombine},
 * each making an independent Bedrock InvokeModel call. The evaluation prompt constrains
 * the judge to return a single numeric score (0–10), normalized to 0.0–1.0.</p>
 *
 * <h3>Cost</h3>
 * <p>Each evaluation invokes Claude 3 Haiku twice (~200 tokens/call) — negligible cost
 * at $0.00025/1K input tokens.</p>
 *
 * @see RetrievalService Provides the chunks scored for relevance
 * @see RagService Provides the generated answer scored for groundedness
 */
@Service
public class EvaluationService {

    private static final Logger log = LoggerFactory.getLogger(EvaluationService.class);

    private final BedrockRuntimeAsyncClient bedrockClient;
    private final RagProperties ragProperties;
    private final ObjectMapper objectMapper;

    @Value("${rag.evaluation.enabled:true}")
    private boolean evaluationEnabled;

    public EvaluationService(BedrockRuntimeAsyncClient bedrockClient,
                             RagProperties ragProperties,
                             ObjectMapper objectMapper) {
        this.bedrockClient = bedrockClient;
        this.ragProperties = ragProperties;
        this.objectMapper = objectMapper;
    }

    public CompletableFuture<EvaluationResponse> evaluate(String query, String answer,
                                                          List<String> retrievedChunks) {
        if (!evaluationEnabled) {
            return CompletableFuture.completedFuture(
                    EvaluationResponse.builder()
                            .query(query)
                            .explanation("Evaluation is disabled")
                            .build());
        }

        long startTime = System.currentTimeMillis();

        return calculateRelevance(query, retrievedChunks)
                .thenCombine(
                        answer != null && !answer.isEmpty()
                                ? calculateGroundedness(answer, retrievedChunks)
                                : CompletableFuture.completedFuture(0.0),
                        (relevance, groundedness) -> EvaluationResponse.builder()
                                .query(query)
                                .relevanceScore(relevance)
                                .groundednessScore(groundedness)
                                .latencyMs(System.currentTimeMillis() - startTime)
                                .build())
                .exceptionally(ex -> {
                    log.error("Evaluation failed for query: {}", query, ex);
                    throw new RagException(RagException.ErrorCode.EVALUATION_FAILED,
                            "Failed to evaluate: " + ex.getMessage(), ex);
                });
    }

    public CompletableFuture<Double> calculateRelevance(String query, List<String> retrievedChunks) {
        String prompt = """
                You are an evaluation assistant. Rate the relevance of the retrieved text chunks to the given query.

                Query: %s

                Retrieved Chunks:
                %s

                Rate the overall relevance on a scale of 0 to 10, where:
                - 0 means completely irrelevant
                - 5 means somewhat relevant
                - 10 means highly relevant and directly answers the query

                Return ONLY a single number between 0 and 10, nothing else.
                """.formatted(query, String.join("\n---\n", retrievedChunks));

        return invokeClaude(prompt)
                .thenApply(response -> {
                    try {
                        double score = Double.parseDouble(response.trim());
                        return Math.min(1.0, Math.max(0.0, score / 10.0));
                    } catch (NumberFormatException e) {
                        log.warn("Could not parse relevance score: {}", response);
                        return 0.5;
                    }
                });
    }

    public CompletableFuture<Double> calculateGroundedness(String answer, List<String> retrievedChunks) {
        String prompt = """
                You are an evaluation assistant. Rate how well the given answer is grounded in (supported by) the source chunks.

                Answer to evaluate: %s

                Source Chunks:
                %s

                Rate the groundedness on a scale of 0 to 10, where:
                - 0 means the answer is completely fabricated with no support from the chunks
                - 5 means the answer is partially supported
                - 10 means every claim in the answer is directly supported by the chunks

                Return ONLY a single number between 0 and 10, nothing else.
                """.formatted(answer, String.join("\n---\n", retrievedChunks));

        return invokeClaude(prompt)
                .thenApply(response -> {
                    try {
                        double score = Double.parseDouble(response.trim());
                        return Math.min(1.0, Math.max(0.0, score / 10.0));
                    } catch (NumberFormatException e) {
                        log.warn("Could not parse groundedness score: {}", response);
                        return 0.5;
                    }
                });
    }

    private CompletableFuture<String> invokeClaude(String prompt) {
        try {
            String requestBody = objectMapper.writeValueAsString(new ClaudeRequest(prompt));

            var request = InvokeModelRequest.builder()
                    .modelId(ragProperties.getModelId())
                    .contentType("application/json")
                    .accept("application/json")
                    .body(SdkBytes.fromString(requestBody, StandardCharsets.UTF_8))
                    .build();

            return bedrockClient.invokeModel(request)
                    .thenApply(response -> {
                        try {
                            String responseBody = response.body().asUtf8String();
                            JsonNode json = objectMapper.readTree(responseBody);
                            return json.path("content").get(0).path("text").asText();
                        } catch (Exception e) {
                            log.error("Failed to parse Claude response", e);
                            throw new RuntimeException("Failed to parse response", e);
                        }
                    });
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    private record ClaudeRequest(
            String anthropic_version,
            int max_tokens,
            List<Message> messages
    ) {
        ClaudeRequest(String prompt) {
            this("bedrock-2023-05-31", 100,
                    List.of(new Message("user", prompt)));
        }

        record Message(String role, String content) {}
    }
}
