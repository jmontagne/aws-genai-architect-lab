package com.jmontagne.bedrock.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import software.amazon.awssdk.services.bedrockruntime.model.BedrockRuntimeException;
import software.amazon.awssdk.services.bedrockruntime.model.ModelNotReadyException;
import software.amazon.awssdk.services.bedrockruntime.model.ModelTimeoutException;
import software.amazon.awssdk.services.bedrockruntime.model.ThrottlingException;
import software.amazon.awssdk.services.bedrockruntime.model.ValidationException;
import software.amazon.awssdk.services.bedrockruntime.model.AccessDeniedException;

import java.time.Instant;

@RestControllerAdvice
public class BedrockExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(BedrockExceptionHandler.class);

    @ExceptionHandler(ThrottlingException.class)
    public ResponseEntity<ErrorResponse> handleThrottling(ThrottlingException ex) {
        log.warn("Bedrock throttling: {}", ex.getMessage());

        ErrorResponse error = new ErrorResponse(
                HttpStatus.TOO_MANY_REQUESTS.value(),
                "THROTTLING",
                "Request rate exceeded. Please retry after a brief delay.",
                Instant.now().toString()
        );

        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", "5")
                .body(error);
    }

    @ExceptionHandler(ModelNotReadyException.class)
    public ResponseEntity<ErrorResponse> handleModelNotReady(ModelNotReadyException ex) {
        log.warn("Model not ready: {}", ex.getMessage());

        ErrorResponse error = new ErrorResponse(
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                "MODEL_NOT_READY",
                "The requested model is not ready. Please try again shortly.",
                Instant.now().toString()
        );

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .header("Retry-After", "30")
                .body(error);
    }

    @ExceptionHandler(ModelTimeoutException.class)
    public ResponseEntity<ErrorResponse> handleModelTimeout(ModelTimeoutException ex) {
        log.error("Model timeout: {}", ex.getMessage());

        ErrorResponse error = new ErrorResponse(
                HttpStatus.GATEWAY_TIMEOUT.value(),
                "MODEL_TIMEOUT",
                "The model request timed out. Consider reducing input size or retrying.",
                Instant.now().toString()
        );

        return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).body(error);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidation(ValidationException ex) {
        log.warn("Validation error: {}", ex.getMessage());

        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "VALIDATION_ERROR",
                "Invalid request parameters: " + ex.getMessage(),
                Instant.now().toString()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        log.error("Access denied: {}", ex.getMessage());

        ErrorResponse error = new ErrorResponse(
                HttpStatus.FORBIDDEN.value(),
                "ACCESS_DENIED",
                "Access to the model is denied. Check IAM permissions and model access.",
                Instant.now().toString()
        );

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler(BedrockRuntimeException.class)
    public ResponseEntity<ErrorResponse> handleBedrockException(BedrockRuntimeException ex) {
        log.error("Bedrock runtime error: {}", ex.getMessage());

        ErrorResponse error = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "BEDROCK_ERROR",
                "An error occurred with the Bedrock service: " + ex.getMessage(),
                Instant.now().toString()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Invalid argument: {}", ex.getMessage());

        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "INVALID_ARGUMENT",
                ex.getMessage(),
                Instant.now().toString()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        log.error("Unexpected error: ", ex);

        ErrorResponse error = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "INTERNAL_ERROR",
                "An unexpected error occurred. Please try again.",
                Instant.now().toString()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    public record ErrorResponse(
            int status,
            String code,
            String message,
            String timestamp
    ) {}
}
