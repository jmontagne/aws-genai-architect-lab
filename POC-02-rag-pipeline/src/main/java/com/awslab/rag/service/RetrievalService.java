package com.awslab.rag.service;

import com.awslab.rag.config.RagProperties;
import com.awslab.rag.exception.RagException;
import com.awslab.rag.model.RetrievalResponse;
import com.awslab.rag.model.RetrievalResponse.RetrievedChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.bedrockagentruntime.BedrockAgentRuntimeAsyncClient;
import software.amazon.awssdk.services.bedrockagentruntime.model.*;

import software.amazon.awssdk.core.document.Document;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Pattern: Raw chunk retrieval via Amazon Bedrock Knowledge Bases Retrieve API.
 *
 * <p>Returns document chunks with relevance scores without generating a final answer.
 * Use this pattern when you need custom prompt engineering, post-processing, or
 * multi-step reasoning over retrieved context.</p>
 *
 * <h3>Search Modes</h3>
 * <ul>
 *   <li><b>SEMANTIC:</b> Vector similarity search using Titan Embeddings V2 (1024 dim,
 *       cosine similarity) — best for natural language queries.</li>
 *   <li><b>HYBRID:</b> Combines semantic search with keyword matching — better for
 *       queries containing specific terms or IDs.</li>
 * </ul>
 *
 * <h3>Metadata Filtering</h3>
 * <p>Supports attribute-based filtering to narrow search scope before vector similarity
 * is computed — reduces noise and improves relevance without additional token cost.</p>
 *
 * @see RagService End-to-end RAG with RetrieveAndGenerate (includes answer generation + citations)
 * @see EvaluationService LLM-as-Judge evaluation of retrieval quality
 */
@Service
public class RetrievalService {

    private static final Logger log = LoggerFactory.getLogger(RetrievalService.class);

    private final BedrockAgentRuntimeAsyncClient client;
    private final RagProperties ragProperties;

    @Value("${rag.retrieval.default-number-of-results:5}")
    private int defaultNumberOfResults;

    public RetrievalService(BedrockAgentRuntimeAsyncClient client, RagProperties ragProperties) {
        this.client = client;
        this.ragProperties = ragProperties;
    }

    public CompletableFuture<RetrievalResponse> retrieve(String query, Integer numberOfResults,
                                                          String searchType, Map<String, String> filter) {
        long startTime = System.currentTimeMillis();
        int resultsToFetch = numberOfResults != null ? numberOfResults : defaultNumberOfResults;

        log.debug("Retrieving {} results for query: {}", resultsToFetch, query);

        var requestBuilder = RetrieveRequest.builder()
                .knowledgeBaseId(ragProperties.getKnowledgeBaseId())
                .retrievalQuery(q -> q.text(query))
                .retrievalConfiguration(config -> config
                        .vectorSearchConfiguration(vs -> {
                            vs.numberOfResults(resultsToFetch);
                            if ("HYBRID".equalsIgnoreCase(searchType)) {
                                vs.overrideSearchType(SearchType.HYBRID);
                            } else if ("SEMANTIC".equalsIgnoreCase(searchType)) {
                                vs.overrideSearchType(SearchType.SEMANTIC);
                            }
                            if (filter != null && !filter.isEmpty()) {
                                vs.filter(buildFilter(filter));
                            }
                        }));

        return client.retrieve(requestBuilder.build())
                .thenApply(response -> {
                    long latency = System.currentTimeMillis() - startTime;
                    List<RetrievedChunk> chunks = response.retrievalResults().stream()
                            .map(this::toRetrievedChunk)
                            .toList();

                    log.debug("Retrieved {} chunks in {}ms", chunks.size(), latency);
                    return new RetrievalResponse(query, chunks, chunks.size(), latency);
                })
                .exceptionally(ex -> {
                    log.error("Retrieval failed for query: {}", query, ex);
                    throw new RagException(RagException.ErrorCode.RETRIEVAL_FAILED,
                            "Failed to retrieve documents: " + ex.getMessage(), ex);
                });
    }

    private RetrievedChunk toRetrievedChunk(KnowledgeBaseRetrievalResult result) {
        String content = result.content() != null ? result.content().text() : "";
        String sourceUri = extractSourceUri(result);
        Double score = result.score();

        Map<String, String> metadata = new HashMap<>();
        if (result.metadata() != null) {
            result.metadata().forEach((k, v) -> metadata.put(k, v.toString()));
        }

        return new RetrievedChunk(content, sourceUri, score, metadata);
    }

    private String extractSourceUri(KnowledgeBaseRetrievalResult result) {
        if (result.location() == null) {
            return "unknown";
        }
        if (result.location().s3Location() != null) {
            return result.location().s3Location().uri();
        }
        return result.location().typeAsString();
    }

    private RetrievalFilter buildFilter(Map<String, String> filterMap) {
        if (filterMap.size() == 1) {
            var entry = filterMap.entrySet().iterator().next();
            return RetrievalFilter.builder()
                    .equalsValue(FilterAttribute.builder()
                            .key(entry.getKey())
                            .value(Document.fromString(entry.getValue()))
                            .build())
                    .build();
        }

        List<RetrievalFilter> filters = filterMap.entrySet().stream()
                .<RetrievalFilter>map(entry -> RetrievalFilter.builder()
                        .equalsValue(FilterAttribute.builder()
                                .key(entry.getKey())
                                .value(Document.fromString(entry.getValue()))
                                .build())
                        .build())
                .toList();

        return RetrievalFilter.builder()
                .andAll(filters)
                .build();
    }
}
