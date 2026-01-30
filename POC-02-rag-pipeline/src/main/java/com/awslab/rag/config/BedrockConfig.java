package com.awslab.rag.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockagent.BedrockAgentAsyncClient;
import software.amazon.awssdk.services.bedrockagentruntime.BedrockAgentRuntimeAsyncClient;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeAsyncClient;

import java.time.Duration;

@Configuration
public class BedrockConfig {

    @Value("${aws.region}")
    private String region;

    @Bean
    public BedrockAgentRuntimeAsyncClient bedrockAgentRuntimeAsyncClient() {
        return BedrockAgentRuntimeAsyncClient.builder()
                .region(Region.of(region))
                .httpClientBuilder(NettyNioAsyncHttpClient.builder()
                        .connectionTimeout(Duration.ofSeconds(30))
                        .readTimeout(Duration.ofSeconds(60))
                        .maxConcurrency(50))
                .build();
    }

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
    public BedrockAgentAsyncClient bedrockAgentAsyncClient() {
        return BedrockAgentAsyncClient.builder()
                .region(Region.of(region))
                .httpClientBuilder(NettyNioAsyncHttpClient.builder()
                        .connectionTimeout(Duration.ofSeconds(30))
                        .readTimeout(Duration.ofSeconds(60))
                        .maxConcurrency(10))
                .build();
    }
}
