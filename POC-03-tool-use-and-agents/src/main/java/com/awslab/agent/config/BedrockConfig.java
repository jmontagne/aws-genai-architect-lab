package com.awslab.agent.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockagentruntime.BedrockAgentRuntimeAsyncClient;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;

import java.time.Duration;

@Configuration
public class BedrockConfig {

    @Value("${aws.region}")
    private String region;

    @Bean
    public BedrockRuntimeAsyncClient bedrockRuntimeAsyncClient() {
        return BedrockRuntimeAsyncClient.builder()
                .region(Region.of(region))
                .httpClientBuilder(NettyNioAsyncHttpClient.builder()
                        .connectionTimeout(Duration.ofSeconds(30))
                        .readTimeout(Duration.ofSeconds(120))
                        .maxConcurrency(50))
                .build();
    }

    @Bean
    public BedrockAgentRuntimeAsyncClient bedrockAgentRuntimeAsyncClient() {
        return BedrockAgentRuntimeAsyncClient.builder()
                .region(Region.of(region))
                .httpClientBuilder(NettyNioAsyncHttpClient.builder()
                        .connectionTimeout(Duration.ofSeconds(30))
                        .readTimeout(Duration.ofSeconds(120))
                        .maxConcurrency(50))
                .build();
    }

    @Bean
    public DynamoDbAsyncClient dynamoDbAsyncClient() {
        return DynamoDbAsyncClient.builder()
                .region(Region.of(region))
                .httpClientBuilder(NettyNioAsyncHttpClient.builder()
                        .connectionTimeout(Duration.ofSeconds(10))
                        .readTimeout(Duration.ofSeconds(30))
                        .maxConcurrency(50))
                .build();
    }
}
