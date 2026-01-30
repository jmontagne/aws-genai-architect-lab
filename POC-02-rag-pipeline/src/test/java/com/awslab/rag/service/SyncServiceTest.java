package com.awslab.rag.service;

import com.awslab.rag.config.RagProperties;
import com.awslab.rag.exception.RagException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.bedrockagent.BedrockAgentAsyncClient;
import software.amazon.awssdk.services.bedrockagent.model.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SyncServiceTest {

    @Mock
    private BedrockAgentAsyncClient bedrockAgentClient;

    @Mock
    private RagProperties ragProperties;

    @InjectMocks
    private SyncService syncService;

    @Test
    void startSync_returnsJobIdStatusStartedAt() {
        when(ragProperties.getKnowledgeBaseId()).thenReturn("kb-123");

        Instant now = Instant.now();
        var job = IngestionJob.builder()
                .ingestionJobId("job-001")
                .status(IngestionJobStatus.STARTING)
                .startedAt(now)
                .build();

        var response = StartIngestionJobResponse.builder()
                .ingestionJob(job)
                .build();

        when(bedrockAgentClient.startIngestionJob(any(StartIngestionJobRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(response));

        Map<String, Object> result = syncService.startSync("ds-001").join();

        assertThat(result).containsEntry("jobId", "job-001");
        assertThat(result).containsEntry("status", "STARTING");
        assertThat(result).containsEntry("startedAt", now.toString());
    }

    @Test
    void startSync_correctKnowledgeBaseAndDataSourceInRequest() {
        when(ragProperties.getKnowledgeBaseId()).thenReturn("kb-456");

        var job = IngestionJob.builder()
                .ingestionJobId("job-002")
                .status(IngestionJobStatus.STARTING)
                .startedAt(Instant.now())
                .build();

        when(bedrockAgentClient.startIngestionJob(any(StartIngestionJobRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(
                        StartIngestionJobResponse.builder().ingestionJob(job).build()));

        syncService.startSync("ds-999").join();

        ArgumentCaptor<StartIngestionJobRequest> captor = ArgumentCaptor.forClass(StartIngestionJobRequest.class);
        verify(bedrockAgentClient).startIngestionJob(captor.capture());

        assertThat(captor.getValue().knowledgeBaseId()).isEqualTo("kb-456");
        assertThat(captor.getValue().dataSourceId()).isEqualTo("ds-999");
    }

    @Test
    void getSyncStatus_inProgress_noCompletedAt() {
        when(ragProperties.getKnowledgeBaseId()).thenReturn("kb-123");

        Instant start = Instant.parse("2024-01-01T00:00:00Z");
        var job = IngestionJob.builder()
                .ingestionJobId("job-001")
                .status(IngestionJobStatus.IN_PROGRESS)
                .startedAt(start)
                .build();

        when(bedrockAgentClient.getIngestionJob(any(GetIngestionJobRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(
                        GetIngestionJobResponse.builder().ingestionJob(job).build()));

        Map<String, Object> result = syncService.getSyncStatus("ds-001", "job-001").join();

        assertThat(result).containsEntry("status", "IN_PROGRESS");
        assertThat(result).doesNotContainKey("completedAt");
    }

    @Test
    void getSyncStatus_complete_includesCompletedAt() {
        when(ragProperties.getKnowledgeBaseId()).thenReturn("kb-123");

        Instant start = Instant.parse("2024-01-01T00:00:00Z");
        Instant end = Instant.parse("2024-01-01T00:05:00Z");
        var job = IngestionJob.builder()
                .ingestionJobId("job-001")
                .status(IngestionJobStatus.COMPLETE)
                .startedAt(start)
                .updatedAt(end)
                .build();

        when(bedrockAgentClient.getIngestionJob(any(GetIngestionJobRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(
                        GetIngestionJobResponse.builder().ingestionJob(job).build()));

        Map<String, Object> result = syncService.getSyncStatus("ds-001", "job-001").join();

        assertThat(result).containsEntry("status", "COMPLETE");
        assertThat(result).containsEntry("completedAt", end.toString());
    }

    @Test
    void getSyncStatus_failed_includesCompletedAtAndFailureReasons() {
        when(ragProperties.getKnowledgeBaseId()).thenReturn("kb-123");

        Instant start = Instant.parse("2024-01-01T00:00:00Z");
        Instant end = Instant.parse("2024-01-01T00:03:00Z");
        var job = IngestionJob.builder()
                .ingestionJobId("job-001")
                .status(IngestionJobStatus.FAILED)
                .startedAt(start)
                .updatedAt(end)
                .failureReasons("Access denied to S3 bucket")
                .build();

        when(bedrockAgentClient.getIngestionJob(any(GetIngestionJobRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(
                        GetIngestionJobResponse.builder().ingestionJob(job).build()));

        Map<String, Object> result = syncService.getSyncStatus("ds-001", "job-001").join();

        assertThat(result).containsEntry("status", "FAILED");
        assertThat(result).containsEntry("completedAt", end.toString());
        assertThat(result).containsKey("failureReasons");
    }

    @Test
    void getSyncStatus_withStatistics_includesDocumentCounts() {
        when(ragProperties.getKnowledgeBaseId()).thenReturn("kb-123");

        var stats = IngestionJobStatistics.builder()
                .numberOfDocumentsScanned(100L)
                .numberOfNewDocumentsIndexed(95L)
                .numberOfDocumentsFailed(5L)
                .build();

        var job = IngestionJob.builder()
                .ingestionJobId("job-001")
                .status(IngestionJobStatus.COMPLETE)
                .startedAt(Instant.now())
                .updatedAt(Instant.now())
                .statistics(stats)
                .build();

        when(bedrockAgentClient.getIngestionJob(any(GetIngestionJobRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(
                        GetIngestionJobResponse.builder().ingestionJob(job).build()));

        Map<String, Object> result = syncService.getSyncStatus("ds-001", "job-001").join();

        assertThat(result).containsEntry("documentsScanned", 100L);
        assertThat(result).containsEntry("documentsIndexed", 95L);
        assertThat(result).containsEntry("documentsFailed", 5L);
    }

    @Test
    void getSyncStatus_nullStatistics_noStatsKeys() {
        when(ragProperties.getKnowledgeBaseId()).thenReturn("kb-123");

        var job = IngestionJob.builder()
                .ingestionJobId("job-001")
                .status(IngestionJobStatus.IN_PROGRESS)
                .startedAt(Instant.now())
                .build();

        when(bedrockAgentClient.getIngestionJob(any(GetIngestionJobRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(
                        GetIngestionJobResponse.builder().ingestionJob(job).build()));

        Map<String, Object> result = syncService.getSyncStatus("ds-001", "job-001").join();

        assertThat(result).doesNotContainKey("documentsScanned");
        assertThat(result).doesNotContainKey("documentsIndexed");
        assertThat(result).doesNotContainKey("documentsFailed");
    }

    @Test
    void getSyncStatus_emptyFailureReasons_notInMap() {
        when(ragProperties.getKnowledgeBaseId()).thenReturn("kb-123");

        var job = IngestionJob.builder()
                .ingestionJobId("job-001")
                .status(IngestionJobStatus.IN_PROGRESS)
                .startedAt(Instant.now())
                .failureReasons(List.of())
                .build();

        when(bedrockAgentClient.getIngestionJob(any(GetIngestionJobRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(
                        GetIngestionJobResponse.builder().ingestionJob(job).build()));

        Map<String, Object> result = syncService.getSyncStatus("ds-001", "job-001").join();

        assertThat(result).doesNotContainKey("failureReasons");
    }

    @Test
    void startSync_sdkException_throwsRagExceptionInternalError() {
        when(ragProperties.getKnowledgeBaseId()).thenReturn("kb-123");

        when(bedrockAgentClient.startIngestionJob(any(StartIngestionJobRequest.class)))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Connection refused")));

        assertThatThrownBy(() -> syncService.startSync("ds-001").join())
                .isInstanceOf(CompletionException.class)
                .hasCauseInstanceOf(RagException.class);
    }

    @Test
    void getSyncStatus_sdkException_throwsRagExceptionInternalError() {
        when(ragProperties.getKnowledgeBaseId()).thenReturn("kb-123");

        when(bedrockAgentClient.getIngestionJob(any(GetIngestionJobRequest.class)))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Timeout")));

        assertThatThrownBy(() -> syncService.getSyncStatus("ds-001", "job-001").join())
                .isInstanceOf(CompletionException.class)
                .hasCauseInstanceOf(RagException.class);
    }
}
