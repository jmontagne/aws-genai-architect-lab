package com.awslab.rag.controller;

import com.awslab.rag.config.RagProperties;
import com.awslab.rag.exception.RagException;
import com.awslab.rag.model.*;
import com.awslab.rag.service.EvaluationService;
import com.awslab.rag.service.RagService;
import com.awslab.rag.service.RetrievalService;
import com.awslab.rag.service.SyncService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import software.amazon.awssdk.services.bedrockagent.BedrockAgentAsyncClient;
import software.amazon.awssdk.services.bedrockagentruntime.BedrockAgentRuntimeAsyncClient;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeAsyncClient;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RagController.class)
@ActiveProfiles("test")
class RagControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RetrievalService retrievalService;

    @MockBean
    private RagService ragService;

    @MockBean
    private EvaluationService evaluationService;

    @MockBean
    private SyncService syncService;

    @MockBean
    private BedrockAgentRuntimeAsyncClient bedrockAgentRuntimeAsyncClient;

    @MockBean
    private BedrockRuntimeAsyncClient bedrockRuntimeAsyncClient;

    @MockBean
    private BedrockAgentAsyncClient bedrockAgentAsyncClient;

    @MockBean
    private RagProperties ragProperties;

    // --- /api/v1/retrieve ---

    @Test
    void retrieve_validRequest_returns200() throws Exception {
        var chunk = new RetrievalResponse.RetrievedChunk("content", "s3://bucket/doc.pdf", 0.95, Map.of());
        var response = new RetrievalResponse("test query", List.of(chunk), 1, 50L);

        when(retrievalService.retrieve(eq("test query"), eq(5), eq("HYBRID"), isNull()))
                .thenReturn(CompletableFuture.completedFuture(response));

        MvcResult mvcResult = mockMvc.perform(post("/api/v1/retrieve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"query":"test query","numberOfResults":5,"searchType":"HYBRID"}
                                """))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.query").value("test query"))
                .andExpect(jsonPath("$.chunks[0].content").value("content"))
                .andExpect(jsonPath("$.totalResults").value(1));
    }

    @Test
    void retrieve_blankQuery_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/retrieve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"query":"","numberOfResults":5}
                                """))
                .andExpect(status().isBadRequest());
    }

    // --- /api/v1/generate ---

    @Test
    void generate_validRequest_returns200() throws Exception {
        var citation = Citation.builder()
                .text("ref text")
                .sourceUri("s3://bucket/doc.pdf")
                .score("0.9")
                .build();
        var response = new GenerateResponse("test query", "Generated answer", List.of(citation), 100L);

        when(ragService.generate(eq("test query"), eq(5), eq(0.0f), eq(1024)))
                .thenReturn(CompletableFuture.completedFuture(response));

        MvcResult mvcResult = mockMvc.perform(post("/api/v1/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"query":"test query","numberOfResults":5,"temperature":0.0,"maxTokens":1024}
                                """))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.query").value("test query"))
                .andExpect(jsonPath("$.answer").value("Generated answer"))
                .andExpect(jsonPath("$.citations[0].text").value("ref text"));
    }

    @Test
    void generate_blankQuery_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"query":"","numberOfResults":5}
                                """))
                .andExpect(status().isBadRequest());
    }

    // --- /api/v1/evaluate ---

    @Test
    void evaluate_validRequest_returns200() throws Exception {
        var response = EvaluationResponse.builder()
                .query("test query")
                .relevanceScore(0.8)
                .groundednessScore(0.7)
                .latencyMs(200L)
                .build();

        when(evaluationService.evaluate(eq("test query"), eq("answer"), eq(List.of("chunk1"))))
                .thenReturn(CompletableFuture.completedFuture(response));

        MvcResult mvcResult = mockMvc.perform(post("/api/v1/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"query":"test query","answer":"answer","retrievedChunks":["chunk1"]}
                                """))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.query").value("test query"))
                .andExpect(jsonPath("$.relevanceScore").value(0.8))
                .andExpect(jsonPath("$.groundednessScore").value(0.7));
    }

    @Test
    void evaluate_blankQuery_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"query":"","answer":"answer","retrievedChunks":["chunk1"]}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void evaluate_emptyChunks_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"query":"test query","answer":"answer","retrievedChunks":[]}
                                """))
                .andExpect(status().isBadRequest());
    }

    // --- /api/v1/sync ---

    @Test
    void startSync_returns200() throws Exception {
        when(syncService.startSync("ds-001"))
                .thenReturn(CompletableFuture.completedFuture(
                        Map.of("jobId", "job-001", "status", "STARTING")));

        MvcResult mvcResult = mockMvc.perform(post("/api/v1/sync/ds-001")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId").value("job-001"))
                .andExpect(jsonPath("$.status").value("STARTING"));
    }

    @Test
    void getSyncStatus_returns200() throws Exception {
        when(syncService.getSyncStatus("ds-001", "job-001"))
                .thenReturn(CompletableFuture.completedFuture(
                        Map.of("jobId", "job-001", "status", "COMPLETE")));

        MvcResult mvcResult = mockMvc.perform(get("/api/v1/sync/ds-001/status/job-001"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId").value("job-001"))
                .andExpect(jsonPath("$.status").value("COMPLETE"));
    }

    // --- /api/v1/health ---

    @Test
    void health_returns200WithStatusUp() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("rag-pipeline"));
    }

    // --- Exception handling through controller ---

    @Test
    void retrieve_serviceThrowsRetrievalFailed_returns500() throws Exception {
        when(retrievalService.retrieve(anyString(), anyInt(), anyString(), any()))
                .thenReturn(CompletableFuture.failedFuture(
                        new RagException(RagException.ErrorCode.RETRIEVAL_FAILED, "Retrieval failed")));

        MvcResult mvcResult = mockMvc.perform(post("/api/v1/retrieve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"query":"test query","numberOfResults":5,"searchType":"HYBRID"}
                                """))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("RETRIEVAL_FAILED"));
    }

    @Test
    void retrieve_serviceThrowsKnowledgeBaseNotFound_returns404() throws Exception {
        when(retrievalService.retrieve(anyString(), anyInt(), anyString(), any()))
                .thenReturn(CompletableFuture.failedFuture(
                        new RagException(RagException.ErrorCode.KNOWLEDGE_BASE_NOT_FOUND, "KB not found")));

        MvcResult mvcResult = mockMvc.perform(post("/api/v1/retrieve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"query":"test query","numberOfResults":5,"searchType":"HYBRID"}
                                """))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("KNOWLEDGE_BASE_NOT_FOUND"));
    }
}
