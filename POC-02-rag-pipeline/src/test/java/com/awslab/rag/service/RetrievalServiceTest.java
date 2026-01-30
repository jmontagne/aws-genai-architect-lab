package com.awslab.rag.service;

import com.awslab.rag.config.RagProperties;
import com.awslab.rag.exception.RagException;
import com.awslab.rag.model.RetrievalResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.document.Document;
import software.amazon.awssdk.services.bedrockagentruntime.BedrockAgentRuntimeAsyncClient;
import software.amazon.awssdk.services.bedrockagentruntime.model.*;

import java.lang.reflect.Field;
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
class RetrievalServiceTest {

    @Mock
    private BedrockAgentRuntimeAsyncClient client;

    @Mock
    private RagProperties ragProperties;

    @InjectMocks
    private RetrievalService retrievalService;

    @BeforeEach
    void setUp() throws Exception {
        Field field = RetrievalService.class.getDeclaredField("defaultNumberOfResults");
        field.setAccessible(true);
        field.set(retrievalService, 5);
    }

    @Test
    void retrieve_happyPath_returnsChunksWithAllFields() throws Exception {
        when(ragProperties.getKnowledgeBaseId()).thenReturn("kb-123");

        var s3Location = RetrievalResultS3Location.builder().uri("s3://bucket/doc.pdf").build();
        var location = RetrievalResultLocation.builder()
                .s3Location(s3Location)
                .type(RetrievalResultLocationType.S3)
                .build();
        var content = RetrievalResultContent.builder().text("chunk text").build();

        var result = KnowledgeBaseRetrievalResult.builder()
                .content(content)
                .location(location)
                .score(0.95)
                .metadata(Map.of("key", Document.fromString("value")))
                .build();

        var response = RetrieveResponse.builder()
                .retrievalResults(List.of(result))
                .build();

        when(client.retrieve(any(RetrieveRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(response));

        RetrievalResponse actual = retrievalService
                .retrieve("test query", 3, "HYBRID", null)
                .join();

        assertThat(actual.query()).isEqualTo("test query");
        assertThat(actual.chunks()).hasSize(1);
        assertThat(actual.chunks().get(0).content()).isEqualTo("chunk text");
        assertThat(actual.chunks().get(0).sourceUri()).isEqualTo("s3://bucket/doc.pdf");
        assertThat(actual.chunks().get(0).score()).isEqualTo(0.95);
        assertThat(actual.chunks().get(0).metadata()).containsKey("key");
        assertThat(actual.totalResults()).isEqualTo(1);
        assertThat(actual.latencyMs()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void retrieve_nullNumberOfResults_usesDefault() throws Exception {
        when(ragProperties.getKnowledgeBaseId()).thenReturn("kb-123");

        var response = RetrieveResponse.builder()
                .retrievalResults(List.of())
                .build();

        when(client.retrieve(any(RetrieveRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(response));

        retrievalService.retrieve("query", null, "HYBRID", null).join();

        ArgumentCaptor<RetrieveRequest> captor = ArgumentCaptor.forClass(RetrieveRequest.class);
        verify(client).retrieve(captor.capture());

        assertThat(captor.getValue().retrievalConfiguration()
                .vectorSearchConfiguration().numberOfResults()).isEqualTo(5);
    }

    @Test
    void retrieve_hybridSearchType_setsOverrideSearchType() throws Exception {
        when(ragProperties.getKnowledgeBaseId()).thenReturn("kb-123");

        var response = RetrieveResponse.builder()
                .retrievalResults(List.of())
                .build();

        when(client.retrieve(any(RetrieveRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(response));

        retrievalService.retrieve("query", 5, "HYBRID", null).join();

        ArgumentCaptor<RetrieveRequest> captor = ArgumentCaptor.forClass(RetrieveRequest.class);
        verify(client).retrieve(captor.capture());

        assertThat(captor.getValue().retrievalConfiguration()
                .vectorSearchConfiguration().overrideSearchType()).isEqualTo(SearchType.HYBRID);
    }

    @Test
    void retrieve_semanticSearchType_setsOverrideSearchType() throws Exception {
        when(ragProperties.getKnowledgeBaseId()).thenReturn("kb-123");

        var response = RetrieveResponse.builder()
                .retrievalResults(List.of())
                .build();

        when(client.retrieve(any(RetrieveRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(response));

        retrievalService.retrieve("query", 5, "SEMANTIC", null).join();

        ArgumentCaptor<RetrieveRequest> captor = ArgumentCaptor.forClass(RetrieveRequest.class);
        verify(client).retrieve(captor.capture());

        assertThat(captor.getValue().retrievalConfiguration()
                .vectorSearchConfiguration().overrideSearchType()).isEqualTo(SearchType.SEMANTIC);
    }

    @Test
    void retrieve_singleFieldFilter_buildsEqualsFilter() throws Exception {
        when(ragProperties.getKnowledgeBaseId()).thenReturn("kb-123");

        var response = RetrieveResponse.builder()
                .retrievalResults(List.of())
                .build();

        when(client.retrieve(any(RetrieveRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(response));

        retrievalService.retrieve("query", 5, "HYBRID", Map.of("department", "HR")).join();

        ArgumentCaptor<RetrieveRequest> captor = ArgumentCaptor.forClass(RetrieveRequest.class);
        verify(client).retrieve(captor.capture());

        var filter = captor.getValue().retrievalConfiguration()
                .vectorSearchConfiguration().filter();
        assertThat(filter).isNotNull();
        assertThat(filter.equalsValue().key()).isEqualTo("department");
        assertThat(filter.equalsValue().value().asString()).isEqualTo("HR");
    }

    @Test
    void retrieve_multiFieldFilter_buildsAndAllFilter() throws Exception {
        when(ragProperties.getKnowledgeBaseId()).thenReturn("kb-123");

        var response = RetrieveResponse.builder()
                .retrievalResults(List.of())
                .build();

        when(client.retrieve(any(RetrieveRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(response));

        retrievalService.retrieve("query", 5, "HYBRID",
                Map.of("department", "HR", "level", "senior")).join();

        ArgumentCaptor<RetrieveRequest> captor = ArgumentCaptor.forClass(RetrieveRequest.class);
        verify(client).retrieve(captor.capture());

        var filter = captor.getValue().retrievalConfiguration()
                .vectorSearchConfiguration().filter();
        assertThat(filter).isNotNull();
        assertThat(filter.andAll()).hasSize(2);
    }

    @Test
    void retrieve_nullFilter_noFilterApplied() throws Exception {
        when(ragProperties.getKnowledgeBaseId()).thenReturn("kb-123");

        var response = RetrieveResponse.builder()
                .retrievalResults(List.of())
                .build();

        when(client.retrieve(any(RetrieveRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(response));

        retrievalService.retrieve("query", 5, "HYBRID", null).join();

        ArgumentCaptor<RetrieveRequest> captor = ArgumentCaptor.forClass(RetrieveRequest.class);
        verify(client).retrieve(captor.capture());

        assertThat(captor.getValue().retrievalConfiguration()
                .vectorSearchConfiguration().filter()).isNull();
    }

    @Test
    void retrieve_nullContent_returnsEmptyString() throws Exception {
        when(ragProperties.getKnowledgeBaseId()).thenReturn("kb-123");

        var result = KnowledgeBaseRetrievalResult.builder()
                .content((RetrievalResultContent) null)
                .score(0.5)
                .build();

        var response = RetrieveResponse.builder()
                .retrievalResults(List.of(result))
                .build();

        when(client.retrieve(any(RetrieveRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(response));

        RetrievalResponse actual = retrievalService
                .retrieve("query", 5, "HYBRID", null).join();

        assertThat(actual.chunks().get(0).content()).isEmpty();
    }

    @Test
    void retrieve_nullLocation_returnsUnknown() throws Exception {
        when(ragProperties.getKnowledgeBaseId()).thenReturn("kb-123");

        var content = RetrievalResultContent.builder().text("text").build();
        var result = KnowledgeBaseRetrievalResult.builder()
                .content(content)
                .location((RetrievalResultLocation) null)
                .score(0.5)
                .build();

        var response = RetrieveResponse.builder()
                .retrievalResults(List.of(result))
                .build();

        when(client.retrieve(any(RetrieveRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(response));

        RetrievalResponse actual = retrievalService
                .retrieve("query", 5, "HYBRID", null).join();

        assertThat(actual.chunks().get(0).sourceUri()).isEqualTo("unknown");
    }

    @Test
    void retrieve_s3Location_returnsS3Uri() throws Exception {
        when(ragProperties.getKnowledgeBaseId()).thenReturn("kb-123");

        var s3Location = RetrievalResultS3Location.builder().uri("s3://my-bucket/file.txt").build();
        var location = RetrievalResultLocation.builder()
                .s3Location(s3Location)
                .type(RetrievalResultLocationType.S3)
                .build();
        var content = RetrievalResultContent.builder().text("text").build();
        var result = KnowledgeBaseRetrievalResult.builder()
                .content(content)
                .location(location)
                .score(0.5)
                .build();

        var response = RetrieveResponse.builder()
                .retrievalResults(List.of(result))
                .build();

        when(client.retrieve(any(RetrieveRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(response));

        RetrievalResponse actual = retrievalService
                .retrieve("query", 5, "HYBRID", null).join();

        assertThat(actual.chunks().get(0).sourceUri()).isEqualTo("s3://my-bucket/file.txt");
    }

    @Test
    void retrieve_sdkException_throwsRagException() {
        when(ragProperties.getKnowledgeBaseId()).thenReturn("kb-123");

        when(client.retrieve(any(RetrieveRequest.class)))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("SDK error")));

        assertThatThrownBy(() -> retrievalService
                .retrieve("query", 5, "HYBRID", null).join())
                .isInstanceOf(CompletionException.class)
                .hasCauseInstanceOf(RagException.class)
                .hasRootCauseMessage("SDK error");
    }
}
