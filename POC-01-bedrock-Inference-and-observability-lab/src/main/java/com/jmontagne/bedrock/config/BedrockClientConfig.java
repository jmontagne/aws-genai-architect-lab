package com.jmontagne.bedrock.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.retry.RetryMode;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.core.retry.backoff.FullJitterBackoffStrategy;
import software.amazon.awssdk.core.retry.conditions.RetryCondition;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeAsyncClient;

import java.time.Duration;

@Configuration
public class BedrockClientConfig {

    private static final Logger log = LoggerFactory.getLogger(BedrockClientConfig.class);

    @Value("${aws.region:us-east-1}")
    private String awsRegion;

    @Value("${bedrock.client.connection-timeout-ms:10000}")
    private int connectionTimeoutMs;

    @Value("${bedrock.client.read-timeout-ms:60000}")
    private int readTimeoutMs;

    @Value("${bedrock.client.max-retries:3}")
    private int maxRetries;

    @Bean
    public BedrockRuntimeAsyncClient bedrockRuntimeAsyncClient() {
        log.info("Initializing BedrockRuntimeAsyncClient for region: {}", awsRegion);

        RetryPolicy retryPolicy = RetryPolicy.builder(RetryMode.ADAPTIVE)
                .numRetries(maxRetries)
                .retryCondition(RetryCondition.defaultRetryCondition())
                .backoffStrategy(FullJitterBackoffStrategy.builder()
                        .baseDelay(Duration.ofMillis(100))
                        .maxBackoffTime(Duration.ofSeconds(20))
                        .build())
                .build();

        ClientOverrideConfiguration overrideConfig = ClientOverrideConfiguration.builder()
                .apiCallTimeout(Duration.ofMillis(readTimeoutMs))
                .apiCallAttemptTimeout(Duration.ofMillis(readTimeoutMs))
                .retryPolicy(retryPolicy)
                .build();

        SdkAsyncHttpClient httpClient = NettyNioAsyncHttpClient.builder()
                .connectionTimeout(Duration.ofMillis(connectionTimeoutMs))
                .readTimeout(Duration.ofMillis(readTimeoutMs))
                .maxConcurrency(50)
                .build();

        return BedrockRuntimeAsyncClient.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .overrideConfiguration(overrideConfig)
                .httpClient(httpClient)
                .build();
    }
}
