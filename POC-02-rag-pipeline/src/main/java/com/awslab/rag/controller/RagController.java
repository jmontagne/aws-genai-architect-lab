package com.awslab.rag.controller;

import com.awslab.rag.model.*;
import com.awslab.rag.service.EvaluationService;
import com.awslab.rag.service.RagService;
import com.awslab.rag.service.RetrievalService;
import com.awslab.rag.service.SyncService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/v1")
public class RagController {

    private static final Logger log = LoggerFactory.getLogger(RagController.class);

    private final RetrievalService retrievalService;
    private final RagService ragService;
    private final EvaluationService evaluationService;
    private final SyncService syncService;

    public RagController(RetrievalService retrievalService,
                         RagService ragService,
                         EvaluationService evaluationService,
                         SyncService syncService) {
        this.retrievalService = retrievalService;
        this.ragService = ragService;
        this.evaluationService = evaluationService;
        this.syncService = syncService;
    }

    @PostMapping("/retrieve")
    public CompletableFuture<ResponseEntity<RetrievalResponse>> retrieve(
            @Valid @RequestBody RetrievalRequest request) {
        log.info("Retrieve request: query='{}', results={}",
                request.query(), request.numberOfResults());

        return retrievalService.retrieve(
                        request.query(),
                        request.numberOfResults(),
                        request.searchType(),
                        request.filter())
                .thenApply(ResponseEntity::ok);
    }

    @PostMapping("/generate")
    public CompletableFuture<ResponseEntity<GenerateResponse>> generate(
            @Valid @RequestBody GenerateRequest request) {
        log.info("Generate request: query='{}', results={}, temp={}",
                request.query(), request.numberOfResults(), request.temperature());

        return ragService.generate(
                        request.query(),
                        request.numberOfResults(),
                        request.temperature(),
                        request.maxTokens())
                .thenApply(ResponseEntity::ok);
    }

    @PostMapping("/evaluate")
    public CompletableFuture<ResponseEntity<EvaluationResponse>> evaluate(
            @Valid @RequestBody EvaluationRequest request) {
        log.info("Evaluate request: query='{}', chunks={}",
                request.query(), request.retrievedChunks().size());

        return evaluationService.evaluate(
                        request.query(),
                        request.answer(),
                        request.retrievedChunks())
                .thenApply(ResponseEntity::ok);
    }

    @PostMapping("/sync/{dataSourceId}")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> startSync(
            @PathVariable String dataSourceId) {
        log.info("Starting sync for data source: {}", dataSourceId);

        return syncService.startSync(dataSourceId)
                .thenApply(ResponseEntity::ok);
    }

    @GetMapping("/sync/{dataSourceId}/status/{jobId}")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> getSyncStatus(
            @PathVariable String dataSourceId,
            @PathVariable String jobId) {
        log.info("Getting sync status for job: {}", jobId);

        return syncService.getSyncStatus(dataSourceId, jobId)
                .thenApply(ResponseEntity::ok);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "rag-pipeline"
        ));
    }
}
