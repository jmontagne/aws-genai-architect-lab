package com.awslab.rag.service;

import com.awslab.rag.config.RagProperties;
import com.awslab.rag.exception.RagException;
import com.awslab.rag.model.GenerateResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
class RagServiceTest {

    @Mock
    private BedrockAgentRuntimeAsyncClient client;

    @Mock
    private RagProperties ragProperties;

    @InjectMocks
    private RagService ragService;

    @BeforeEach
    void setUp() throws Exception {
        setField("region", "us-east-1");
        setField("defaultTemperature", 0.0f);
        setField("defaultMaxTokens", 1024);
    }

    private void setField(String name, Object value) throws Exception {
        Field field = RagService.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(ragService, value);
    }

    private RetrieveAndGenerateResponse buildResponse(String outputText,
                                                       List<software.amazon.awssdk.services.bedrockagentruntime.model.Citation> citations) {
        var builder = RetrieveAndGenerateResponse.builder();
        if (outputText != null) {
            builder.output(RetrieveAndGenerateOutput.builder().text(outputText).build());
        }
        if (citations != null) {
            builder.citations(citations);
        }
        return builder.build();
    }

    @Test
    void generate_happyPath_returnsAnswerAndCitations() throws Exception {
        when(ragProperties.getKnowledgeBaseId()).thenReturn("kb-123");
        when(ragProperties.getModelArn("us-east-1")).thenReturn("arn:aws:bedrock:us-east-1::foundation-model/anthropic.claude-3-sonnet-20240229-v1:0");

        var s3Location = RetrievalResultS3Location.builder().uri("s3://bucket/doc.pdf").build();
        var refLocation = RetrievalResultLocation.builder()
                .s3Location(s3Location)
                .type(RetrievalResultLocationType.S3)
                .build();
        var refContent = RetrievalResultContent.builder().text("ref text").build();
        var reference = RetrievedReference.builder()
                .content(refContent)
                .location(refLocation)
                .metadata(Map.of("score", software.amazon.awssdk.core.document.Document.fromString("0.9")))
                .build();

        var span = Span.builder().start(0).end(10).build();
        var textResponsePart = TextResponsePart.builder().span(span).build();
        var generatedResponsePart = GeneratedResponsePart.builder().textResponsePart(textResponsePart).build();

        var citation = software.amazon.awssdk.services.bedrockagentruntime.model.Citation.builder()
                .retrievedReferences(List.of(reference))
                .generatedResponsePart(generatedResponsePart)
                .build();

        var response = buildResponse("Generated answer", List.of(citation));

        when(client.retrieveAndGenerate(any(RetrieveAndGenerateRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(response));

        GenerateResponse actual = ragService.generate("test query", 5, 0.5f, 512).join();

        assertThat(actual.query()).isEqualTo("test query");
        assertThat(actual.answer()).isEqualTo("Generated answer");
        assertThat(actual.citations()).hasSize(1);
        assertThat(actual.citations().get(0).text()).isEqualTo("ref text");
        assertThat(actual.citations().get(0).generatedSpan()).isNotNull();
        assertThat(actual.citations().get(0).generatedSpan().start()).isEqualTo(0);
        assertThat(actual.citations().get(0).generatedSpan().end()).isEqualTo(10);
        assertThat(actual.latencyMs()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void generate_nullParameters_usesDefaults() throws Exception {
        when(ragProperties.getKnowledgeBaseId()).thenReturn("kb-123");
        when(ragProperties.getModelArn("us-east-1")).thenReturn("arn:model");

        var response = buildResponse("answer", null);

        when(client.retrieveAndGenerate(any(RetrieveAndGenerateRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(response));

        ragService.generate("query", null, null, null).join();

        ArgumentCaptor<RetrieveAndGenerateRequest> captor = ArgumentCaptor.forClass(RetrieveAndGenerateRequest.class);
        verify(client).retrieveAndGenerate(captor.capture());

        var kbConfig = captor.getValue().retrieveAndGenerateConfiguration().knowledgeBaseConfiguration();
        assertThat(kbConfig.retrievalConfiguration().vectorSearchConfiguration().numberOfResults()).isEqualTo(5);
        assertThat(kbConfig.generationConfiguration().inferenceConfig().textInferenceConfig().temperature()).isEqualTo(0.0f);
        assertThat(kbConfig.generationConfiguration().inferenceConfig().textInferenceConfig().maxTokens()).isEqualTo(1024);
    }

    @Test
    void generate_modelArnConstruction_verified() throws Exception {
        when(ragProperties.getKnowledgeBaseId()).thenReturn("kb-123");
        when(ragProperties.getModelArn("us-east-1")).thenReturn("arn:aws:bedrock:us-east-1::foundation-model/anthropic.claude-3-sonnet-20240229-v1:0");

        var response = buildResponse("answer", null);

        when(client.retrieveAndGenerate(any(RetrieveAndGenerateRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(response));

        ragService.generate("query", 5, 0.0f, 1024).join();

        ArgumentCaptor<RetrieveAndGenerateRequest> captor = ArgumentCaptor.forClass(RetrieveAndGenerateRequest.class);
        verify(client).retrieveAndGenerate(captor.capture());

        assertThat(captor.getValue().retrieveAndGenerateConfiguration()
                .knowledgeBaseConfiguration().modelArn())
                .isEqualTo("arn:aws:bedrock:us-east-1::foundation-model/anthropic.claude-3-sonnet-20240229-v1:0");
    }

    @Test
    void generate_citationWithGeneratedSpan_includesStartEnd() throws Exception {
        when(ragProperties.getKnowledgeBaseId()).thenReturn("kb-123");
        when(ragProperties.getModelArn("us-east-1")).thenReturn("arn:model");

        var reference = RetrievedReference.builder()
                .content(RetrievalResultContent.builder().text("text").build())
                .build();

        var span = Span.builder().start(5).end(20).build();
        var citation = software.amazon.awssdk.services.bedrockagentruntime.model.Citation.builder()
                .retrievedReferences(List.of(reference))
                .generatedResponsePart(GeneratedResponsePart.builder()
                        .textResponsePart(TextResponsePart.builder().span(span).build())
                        .build())
                .build();

        var response = buildResponse("answer", List.of(citation));

        when(client.retrieveAndGenerate(any(RetrieveAndGenerateRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(response));

        GenerateResponse actual = ragService.generate("query", 5, 0.0f, 1024).join();

        assertThat(actual.citations().get(0).generatedSpan().start()).isEqualTo(5);
        assertThat(actual.citations().get(0).generatedSpan().end()).isEqualTo(20);
    }

    @Test
    void generate_citationWithoutGeneratedResponsePart_spanIsNull() throws Exception {
        when(ragProperties.getKnowledgeBaseId()).thenReturn("kb-123");
        when(ragProperties.getModelArn("us-east-1")).thenReturn("arn:model");

        var reference = RetrievedReference.builder()
                .content(RetrievalResultContent.builder().text("text").build())
                .build();

        var citation = software.amazon.awssdk.services.bedrockagentruntime.model.Citation.builder()
                .retrievedReferences(List.of(reference))
                .build();

        var response = buildResponse("answer", List.of(citation));

        when(client.retrieveAndGenerate(any(RetrieveAndGenerateRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(response));

        GenerateResponse actual = ragService.generate("query", 5, 0.0f, 1024).join();

        assertThat(actual.citations().get(0).generatedSpan()).isNull();
    }

    @Test
    void generate_nullCitations_returnsEmptyList() throws Exception {
        when(ragProperties.getKnowledgeBaseId()).thenReturn("kb-123");
        when(ragProperties.getModelArn("us-east-1")).thenReturn("arn:model");

        var response = buildResponse("answer", null);

        when(client.retrieveAndGenerate(any(RetrieveAndGenerateRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(response));

        GenerateResponse actual = ragService.generate("query", 5, 0.0f, 1024).join();

        assertThat(actual.citations()).isEmpty();
    }

    @Test
    void generate_nullRetrievedReferences_skipped() throws Exception {
        when(ragProperties.getKnowledgeBaseId()).thenReturn("kb-123");
        when(ragProperties.getModelArn("us-east-1")).thenReturn("arn:model");

        var citation = software.amazon.awssdk.services.bedrockagentruntime.model.Citation.builder()
                .build();

        var response = buildResponse("answer", List.of(citation));

        when(client.retrieveAndGenerate(any(RetrieveAndGenerateRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(response));

        GenerateResponse actual = ragService.generate("query", 5, 0.0f, 1024).join();

        assertThat(actual.citations()).isEmpty();
    }

    @Test
    void generate_nullOutput_returnsEmptyAnswer() throws Exception {
        when(ragProperties.getKnowledgeBaseId()).thenReturn("kb-123");
        when(ragProperties.getModelArn("us-east-1")).thenReturn("arn:model");

        var response = buildResponse(null, null);

        when(client.retrieveAndGenerate(any(RetrieveAndGenerateRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(response));

        GenerateResponse actual = ragService.generate("query", 5, 0.0f, 1024).join();

        assertThat(actual.answer()).isEmpty();
    }

    @Test
    void generate_nullContentAndLocationInReference_usesDefaults() throws Exception {
        when(ragProperties.getKnowledgeBaseId()).thenReturn("kb-123");
        when(ragProperties.getModelArn("us-east-1")).thenReturn("arn:model");

        var reference = RetrievedReference.builder().build();
        var citation = software.amazon.awssdk.services.bedrockagentruntime.model.Citation.builder()
                .retrievedReferences(List.of(reference))
                .build();

        var response = buildResponse("answer", List.of(citation));

        when(client.retrieveAndGenerate(any(RetrieveAndGenerateRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(response));

        GenerateResponse actual = ragService.generate("query", 5, 0.0f, 1024).join();

        assertThat(actual.citations().get(0).text()).isEmpty();
        assertThat(actual.citations().get(0).sourceUri()).isEqualTo("unknown");
    }

    @Test
    void generate_sdkException_throwsRagException() {
        when(ragProperties.getKnowledgeBaseId()).thenReturn("kb-123");
        when(ragProperties.getModelArn("us-east-1")).thenReturn("arn:model");

        when(client.retrieveAndGenerate(any(RetrieveAndGenerateRequest.class)))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("SDK failure")));

        assertThatThrownBy(() -> ragService.generate("query", 5, 0.0f, 1024).join())
                .isInstanceOf(CompletionException.class)
                .hasCauseInstanceOf(RagException.class)
                .hasRootCauseMessage("SDK failure");
    }
}
