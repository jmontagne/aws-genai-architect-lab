package com.awslab.agent.exception;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @InjectMocks
    private GlobalExceptionHandler handler;

    @Test
    void handleAgentException_toolNotFound_returns404() {
        AgentException ex = new AgentException(AgentException.ErrorCode.TOOL_NOT_FOUND, "Tool not found");

        ResponseEntity<Map<String, Object>> response = handler.handleAgentException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().get("error")).isEqualTo("TOOL_NOT_FOUND");
    }

    @Test
    void handleAgentException_throttling_returns429() {
        AgentException ex = new AgentException(AgentException.ErrorCode.THROTTLING, "Rate limited");

        ResponseEntity<Map<String, Object>> response = handler.handleAgentException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(response.getBody().get("error")).isEqualTo("THROTTLING");
    }

    @Test
    void handleAgentException_maxIterationsExceeded_returns422() {
        AgentException ex = new AgentException(AgentException.ErrorCode.MAX_ITERATIONS_EXCEEDED,
                "Exceeded max iterations");

        ResponseEntity<Map<String, Object>> response = handler.handleAgentException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody().get("error")).isEqualTo("MAX_ITERATIONS_EXCEEDED");
    }

    @Test
    void handleAgentException_converseApiFailed_returns500() {
        AgentException ex = new AgentException(AgentException.ErrorCode.CONVERSE_API_FAILED, "API error");

        ResponseEntity<Map<String, Object>> response = handler.handleAgentException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().get("error")).isEqualTo("CONVERSE_API_FAILED");
    }

    @Test
    void handleAgentException_internalError_returns500() {
        AgentException ex = new AgentException(AgentException.ErrorCode.INTERNAL_ERROR, "Internal error");

        ResponseEntity<Map<String, Object>> response = handler.handleAgentException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().get("error")).isEqualTo("INTERNAL_ERROR");
    }

    @Test
    void handleAgentException_responseContainsTimestamp() {
        AgentException ex = new AgentException(AgentException.ErrorCode.TOOL_NOT_FOUND, "Not found");

        ResponseEntity<Map<String, Object>> response = handler.handleAgentException(ex);

        assertThat(response.getBody()).containsKey("timestamp");
        assertThat(response.getBody().get("message")).isEqualTo("Not found");
    }

    @Test
    void handleGenericException_returns500() {
        Exception ex = new RuntimeException("Unexpected error");

        ResponseEntity<Map<String, Object>> response = handler.handleGenericException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().get("error")).isEqualTo("INTERNAL_ERROR");
    }
}
