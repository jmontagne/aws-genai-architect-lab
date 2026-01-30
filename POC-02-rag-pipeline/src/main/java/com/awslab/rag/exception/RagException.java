package com.awslab.rag.exception;

public class RagException extends RuntimeException {

    private final ErrorCode errorCode;

    public RagException(String message) {
        super(message);
        this.errorCode = ErrorCode.INTERNAL_ERROR;
    }

    public RagException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = ErrorCode.INTERNAL_ERROR;
    }

    public RagException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public RagException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public enum ErrorCode {
        RETRIEVAL_FAILED,
        GENERATION_FAILED,
        EVALUATION_FAILED,
        KNOWLEDGE_BASE_NOT_FOUND,
        MODEL_INVOCATION_FAILED,
        THROTTLING,
        INTERNAL_ERROR
    }
}
