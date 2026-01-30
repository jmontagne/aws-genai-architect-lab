package com.awslab.rag.service;

import com.awslab.rag.config.RagProperties;
import com.awslab.rag.exception.RagException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.bedrockagent.BedrockAgentAsyncClient;
import software.amazon.awssdk.services.bedrockagent.model.StartIngestionJobRequest;
import software.amazon.awssdk.services.bedrockagent.model.GetIngestionJobRequest;
import software.amazon.awssdk.services.bedrockagent.model.IngestionJobStatus;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
public class SyncService {

    private static final Logger log = LoggerFactory.getLogger(SyncService.class);

    private final BedrockAgentAsyncClient bedrockAgentClient;
    private final RagProperties ragProperties;

    public SyncService(BedrockAgentAsyncClient bedrockAgentClient, RagProperties ragProperties) {
        this.bedrockAgentClient = bedrockAgentClient;
        this.ragProperties = ragProperties;
    }

    public CompletableFuture<Map<String, Object>> startSync(String dataSourceId) {
        log.info("Starting ingestion job for knowledge base: {} data source: {}",
                ragProperties.getKnowledgeBaseId(), dataSourceId);

        var request = StartIngestionJobRequest.builder()
                .knowledgeBaseId(ragProperties.getKnowledgeBaseId())
                .dataSourceId(dataSourceId)
                .build();

        return bedrockAgentClient.startIngestionJob(request)
                .thenApply(response -> {
                    var job = response.ingestionJob();
                    log.info("Ingestion job started: {}", job.ingestionJobId());
                    return Map.of(
                            "jobId", job.ingestionJobId(),
                            "status", job.status().toString(),
                            "startedAt", job.startedAt().toString()
                    );
                })
                .exceptionally(ex -> {
                    log.error("Failed to start ingestion job", ex);
                    throw new RagException(RagException.ErrorCode.INTERNAL_ERROR,
                            "Failed to start sync: " + ex.getMessage(), ex);
                });
    }

    public CompletableFuture<Map<String, Object>> getSyncStatus(String dataSourceId, String jobId) {
        var request = GetIngestionJobRequest.builder()
                .knowledgeBaseId(ragProperties.getKnowledgeBaseId())
                .dataSourceId(dataSourceId)
                .ingestionJobId(jobId)
                .build();

        return bedrockAgentClient.getIngestionJob(request)
                .thenApply(response -> {
                    var job = response.ingestionJob();
                    var result = new java.util.HashMap<String, Object>();
                    result.put("jobId", job.ingestionJobId());
                    result.put("status", job.status().toString());
                    result.put("startedAt", job.startedAt().toString());

                    if (job.status() == IngestionJobStatus.COMPLETE ||
                            job.status() == IngestionJobStatus.FAILED) {
                        if (job.updatedAt() != null) {
                            result.put("completedAt", job.updatedAt().toString());
                        }
                    }

                    if (job.statistics() != null) {
                        result.put("documentsScanned", job.statistics().numberOfDocumentsScanned());
                        result.put("documentsIndexed", job.statistics().numberOfNewDocumentsIndexed());
                        result.put("documentsFailed", job.statistics().numberOfDocumentsFailed());
                    }

                    if (job.failureReasons() != null && !job.failureReasons().isEmpty()) {
                        result.put("failureReasons", job.failureReasons());
                    }

                    return result;
                })
                .exceptionally(ex -> {
                    log.error("Failed to get ingestion job status", ex);
                    throw new RagException(RagException.ErrorCode.INTERNAL_ERROR,
                            "Failed to get sync status: " + ex.getMessage(), ex);
                });
    }
}
