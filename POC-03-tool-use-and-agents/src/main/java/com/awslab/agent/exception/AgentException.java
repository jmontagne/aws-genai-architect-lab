package com.awslab.agent.exception;

public class AgentException extends RuntimeException {

    private final ErrorCode errorCode;

    public AgentException(String message) {
        super(message);
        this.errorCode = ErrorCode.INTERNAL_ERROR;
    }

    public AgentException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = ErrorCode.INTERNAL_ERROR;
    }

    public AgentException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public AgentException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public enum ErrorCode {
        TOOL_EXECUTION_FAILED,
        CONVERSE_API_FAILED,
        AGENT_INVOCATION_FAILED,
        MAX_ITERATIONS_EXCEEDED,
        TOOL_NOT_FOUND,
        DYNAMODB_QUERY_FAILED,
        THROTTLING,
        INTERNAL_ERROR
    }
}
