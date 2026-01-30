package com.awslab.rag.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void handleRagException_knowledgeBaseNotFound_returns404() {
        var ex = new RagException(RagException.ErrorCode.KNOWLEDGE_BASE_NOT_FOUND, "KB not found");

        ResponseEntity<Map<String, Object>> response = handler.handleRagException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsEntry("error", "KNOWLEDGE_BASE_NOT_FOUND");
        assertThat(response.getBody()).containsEntry("message", "KB not found");
    }

    @Test
    void handleRagException_throttling_returns429() {
        var ex = new RagException(RagException.ErrorCode.THROTTLING, "Rate limited");

        ResponseEntity<Map<String, Object>> response = handler.handleRagException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(response.getBody()).containsEntry("error", "THROTTLING");
    }

    @Test
    void handleRagException_retrievalFailed_returns500() {
        var ex = new RagException(RagException.ErrorCode.RETRIEVAL_FAILED, "Retrieval error");

        ResponseEntity<Map<String, Object>> response = handler.handleRagException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).containsEntry("error", "RETRIEVAL_FAILED");
    }

    @Test
    void handleRagException_generationFailed_returns500() {
        var ex = new RagException(RagException.ErrorCode.GENERATION_FAILED, "Generation error");

        ResponseEntity<Map<String, Object>> response = handler.handleRagException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void handleRagException_evaluationFailed_returns500() {
        var ex = new RagException(RagException.ErrorCode.EVALUATION_FAILED, "Eval error");

        ResponseEntity<Map<String, Object>> response = handler.handleRagException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void handleRagException_internalError_returns500() {
        var ex = new RagException(RagException.ErrorCode.INTERNAL_ERROR, "Internal error");

        ResponseEntity<Map<String, Object>> response = handler.handleRagException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).containsEntry("error", "INTERNAL_ERROR");
    }

    @Test
    void handleRagException_bodyContainsTimestamp() {
        var ex = new RagException(RagException.ErrorCode.INTERNAL_ERROR, "test");

        ResponseEntity<Map<String, Object>> response = handler.handleRagException(ex);

        assertThat(response.getBody()).containsKey("timestamp");
    }

    @Test
    void handleValidationException_returns400WithFieldErrors() throws Exception {
        var bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "query", "Query is required"));

        var methodParam = new MethodParameter(
                GlobalExceptionHandlerTest.class.getDeclaredMethod("handleValidationException_returns400WithFieldErrors"), -1);
        var ex = new MethodArgumentNotValidException(methodParam, bindingResult);

        ResponseEntity<Map<String, Object>> response = handler.handleValidationException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("error", "VALIDATION_ERROR");
        assertThat((String) response.getBody().get("message")).contains("query");
        assertThat((String) response.getBody().get("message")).contains("Query is required");
    }

    @Test
    void handleGenericException_returns500WithGenericMessage() {
        var ex = new RuntimeException("Something broke");

        ResponseEntity<Map<String, Object>> response = handler.handleGenericException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).containsEntry("error", "INTERNAL_ERROR");
        assertThat(response.getBody()).containsEntry("message", "An unexpected error occurred");
        assertThat(response.getBody()).containsKey("timestamp");
    }
}
