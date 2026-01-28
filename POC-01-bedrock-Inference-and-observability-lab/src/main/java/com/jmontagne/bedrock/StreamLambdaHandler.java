package com.jmontagne.bedrock;

import com.amazonaws.serverless.exceptions.ContainerInitializationException;
import com.amazonaws.serverless.proxy.model.HttpApiV2ProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.spring.SpringBootLambdaContainerHandler;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.lambda.powertools.logging.Logging;
import software.amazon.lambda.powertools.metrics.Metrics;
import software.amazon.lambda.powertools.tracing.Tracing;

/**
 * AWS Lambda handler for Spring Boot application.
 *
 * Uses AWS Serverless Java Container to run Spring Boot on Lambda.
 * Configured with AWS Lambda Powertools for:
 * - Structured logging (JSON format)
 * - Distributed tracing (X-Ray)
 * - Custom metrics (CloudWatch)
 *
 * SnapStart is enabled in Terraform for fast cold starts (~200ms instead of ~5s).
 *
 * Uses HttpApiV2ProxyRequest for API Gateway HTTP API (v2 payload format).
 */
public class StreamLambdaHandler implements RequestHandler<HttpApiV2ProxyRequest, AwsProxyResponse> {

    private static final Logger log = LoggerFactory.getLogger(StreamLambdaHandler.class);

    private static final SpringBootLambdaContainerHandler<HttpApiV2ProxyRequest, AwsProxyResponse> handler;

    static {
        try {
            log.info("Initializing Spring Boot Lambda container...");
            // Use HTTP API v2 handler for API Gateway HTTP API
            handler = SpringBootLambdaContainerHandler.getHttpApiV2ProxyHandler(BedrockInferenceApplication.class);
            log.info("Spring Boot Lambda container initialized successfully");
        } catch (ContainerInitializationException e) {
            log.error("Failed to initialize Spring Boot Lambda container", e);
            throw new RuntimeException("Could not initialize Spring Boot application", e);
        }
    }

    /**
     * Lambda entry point.
     *
     * Annotations from AWS Lambda Powertools:
     * - @Logging: Adds correlation ID, structures logs as JSON
     * - @Tracing: Creates X-Ray segments for distributed tracing
     * - @Metrics: Captures custom CloudWatch metrics
     */
    @Override
    @Logging(logEvent = true)
    @Tracing
    @Metrics(captureColdStart = true)
    public AwsProxyResponse handleRequest(HttpApiV2ProxyRequest input, Context context) {
        log.info("Processing request: {} {}",
                input.getRequestContext().getHttp().getMethod(),
                input.getRequestContext().getHttp().getPath());

        AwsProxyResponse response = handler.proxy(input, context);

        log.info("Request completed with status: {}", response.getStatusCode());
        return response;
    }
}
