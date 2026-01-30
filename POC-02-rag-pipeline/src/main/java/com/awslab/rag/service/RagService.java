package com.awslab.rag.service;

import com.awslab.rag.config.RagProperties;
import com.awslab.rag.exception.RagException;
import com.awslab.rag.model.Citation;
import com.awslab.rag.model.GenerateResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.bedrockagentruntime.BedrockAgentRuntimeAsyncClient;
import software.amazon.awssdk.services.bedrockagentruntime.model.*;

import software.amazon.awssdk.core.document.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class RagService {

    private static final Logger log = LoggerFactory.getLogger(RagService.class);

    private final BedrockAgentRuntimeAsyncClient client;
    private final RagProperties ragProperties;

    @Value("${aws.region}")
    private String region;

    @Value("${rag.generation.temperature:0.0}")
    private float defaultTemperature;

    @Value("${rag.generation.max-tokens:1024}")
    private int defaultMaxTokens;

    public RagService(BedrockAgentRuntimeAsyncClient client, RagProperties ragProperties) {
        this.client = client;
        this.ragProperties = ragProperties;
    }

    public CompletableFuture<GenerateResponse> generate(String query, Integer numberOfResults,
                                                         Float temperature, Integer maxTokens) {
        long startTime = System.currentTimeMillis();
        int results = numberOfResults != null ? numberOfResults : 5;
        float temp = temperature != null ? temperature : defaultTemperature;
        int tokens = maxTokens != null ? maxTokens : defaultMaxTokens;

        log.debug("Generating response for query: {} (results={}, temp={}, maxTokens={})",
                query, results, temp, tokens);

        String modelArn = ragProperties.getModelArn(region);

        var request = RetrieveAndGenerateRequest.builder()
                .input(i -> i.text(query))
                .retrieveAndGenerateConfiguration(config -> config
                        .type(RetrieveAndGenerateType.KNOWLEDGE_BASE)
                        .knowledgeBaseConfiguration(kb -> kb
                                .knowledgeBaseId(ragProperties.getKnowledgeBaseId())
                                .modelArn(modelArn)
                                .retrievalConfiguration(r -> r
                                        .vectorSearchConfiguration(vs -> vs
                                                .numberOfResults(results)
                                                .overrideSearchType(SearchType.HYBRID)))
                                .generationConfiguration(g -> g
                                        .inferenceConfig(ic -> ic
                                                .textInferenceConfig(t -> t
                                                        .temperature(temp)
                                                        .maxTokens(tokens))))))
                .build();

        return client.retrieveAndGenerate(request)
                .thenApply(response -> {
                    long latency = System.currentTimeMillis() - startTime;
                    String answer = response.output() != null ? response.output().text() : "";
                    List<Citation> citations = extractCitations(response);

                    log.debug("Generated response with {} citations in {}ms", citations.size(), latency);
                    return new GenerateResponse(query, answer, citations, latency);
                })
                .exceptionally(ex -> {
                    log.error("Generation failed for query: {}", query, ex);
                    throw new RagException(RagException.ErrorCode.GENERATION_FAILED,
                            "Failed to generate response: " + ex.getMessage(), ex);
                });
    }

    private List<Citation> extractCitations(RetrieveAndGenerateResponse response) {
        List<Citation> citations = new ArrayList<>();

        if (response.citations() == null) {
            return citations;
        }

        for (var citation : response.citations()) {
            if (citation.retrievedReferences() == null) {
                continue;
            }

            for (var reference : citation.retrievedReferences()) {
                String text = reference.content() != null ? reference.content().text() : "";
                String sourceUri = extractSourceUri(reference);
                String score = reference.metadata() != null
                        ? reference.metadata().getOrDefault("score", Document.fromString("N/A")).toString()
                        : "N/A";

                var citationBuilder = Citation.builder()
                        .text(text)
                        .sourceUri(sourceUri)
                        .score(score);

                if (citation.generatedResponsePart() != null
                        && citation.generatedResponsePart().textResponsePart() != null
                        && citation.generatedResponsePart().textResponsePart().span() != null) {
                    var span = citation.generatedResponsePart().textResponsePart().span();
                    citationBuilder.generatedSpan(span.start(), span.end());
                }

                citations.add(citationBuilder.build());
            }
        }

        return citations;
    }

    private String extractSourceUri(RetrievedReference reference) {
        if (reference.location() == null) {
            return "unknown";
        }
        if (reference.location().s3Location() != null) {
            return reference.location().s3Location().uri();
        }
        return reference.location().typeAsString();
    }
}
