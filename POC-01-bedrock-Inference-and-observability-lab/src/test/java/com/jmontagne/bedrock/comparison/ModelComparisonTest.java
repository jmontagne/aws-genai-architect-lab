package com.jmontagne.bedrock.comparison;

import com.jmontagne.bedrock.client.BedrockStreamingClient;
import com.jmontagne.bedrock.model.InferenceParameters;
import com.jmontagne.bedrock.model.InferenceRequest;
import com.jmontagne.bedrock.model.ModelType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeAsyncClient;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named = "AWS_ACCESS_KEY_ID", matches = ".+")
@DisplayName("Model Comparison Tests - Claude 3.5 Sonnet vs Claude 3 Haiku")
class ModelComparisonTest {

    private static final Logger log = LoggerFactory.getLogger(ModelComparisonTest.class);

    private static final String TEST_MESSAGE = "What is the secret to making a perfect French omelette?";
    private static final InferenceParameters DETERMINISTIC_PARAMS = InferenceParameters.deterministic()
            .withMaxTokens(512);

    private BedrockStreamingClient streamingClient;

    @BeforeEach
    void setUp() {
        BedrockRuntimeAsyncClient bedrockClient = BedrockRuntimeAsyncClient.builder()
                .region(Region.US_EAST_1)
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();

        streamingClient = new BedrockStreamingClient(bedrockClient);
    }

    @Test
    @DisplayName("Compare Claude 3.5 Sonnet and Claude 3 Haiku streaming responses")
    void compareModelsStreaming() {
        log.info("=== Starting Model Comparison Test ===");
        log.info("Test message: {}", TEST_MESSAGE);
        log.info("Parameters: temperature=0 (deterministic), maxTokens=512");

        // Test Claude 3.5 Sonnet
        ModelResult claudeResult = testModel(ModelType.CLAUDE_3_5_SONNET);

        // Test Claude 3 Haiku
        ModelResult haikuResult = testModel(ModelType.CLAUDE_3_HAIKU);

        // Log comparison results
        logComparisonResults(claudeResult, haikuResult);

        // Assertions
        assertNotNull(claudeResult.response(), "Claude response should not be null");
        assertNotNull(haikuResult.response(), "Haiku response should not be null");
        assertFalse(claudeResult.response().isEmpty(), "Claude response should not be empty");
        assertFalse(haikuResult.response().isEmpty(), "Haiku response should not be empty");
        assertTrue(claudeResult.ttftMs() > 0, "Claude TTFT should be positive");
        assertTrue(haikuResult.ttftMs() > 0, "Haiku TTFT should be positive");
    }

    @Test
    @DisplayName("Test Claude 3.5 Sonnet streaming with Jacques Montagne persona")
    void testClaudeWithJacquesMontagne() {
        InferenceRequest request = InferenceRequest.withJacquesMontagne(
                TEST_MESSAGE,
                ModelType.CLAUDE_3_5_SONNET,
                DETERMINISTIC_PARAMS
        );

        StringBuilder response = new StringBuilder();
        AtomicLong ttft = new AtomicLong(0);
        AtomicBoolean firstToken = new AtomicBoolean(false);
        long startTime = System.currentTimeMillis();

        streamingClient.streamConverse(request)
                .doOnNext(chunk -> {
                    if (!firstToken.getAndSet(true)) {
                        ttft.set(System.currentTimeMillis() - startTime);
                    }
                    response.append(chunk);
                })
                .blockLast(Duration.ofSeconds(60));

        log.info("Claude Response (Jacques Montagne persona):");
        log.info("TTFT: {}ms", ttft.get());
        log.info("Response length: {} chars", response.length());
        log.info("Response preview: {}...", response.substring(0, Math.min(200, response.length())));

        assertTrue(response.length() > 0, "Response should not be empty");
        assertTrue(ttft.get() > 0, "TTFT should be recorded");
    }

    @Test
    @DisplayName("Test Claude 3 Haiku streaming with Jacques Montagne persona")
    void testHaikuWithJacquesMontagne() {
        InferenceRequest request = InferenceRequest.withJacquesMontagne(
                TEST_MESSAGE,
                ModelType.CLAUDE_3_HAIKU,
                DETERMINISTIC_PARAMS
        );

        StringBuilder response = new StringBuilder();
        AtomicLong ttft = new AtomicLong(0);
        AtomicBoolean firstToken = new AtomicBoolean(false);
        long startTime = System.currentTimeMillis();

        streamingClient.streamConverse(request)
                .doOnNext(chunk -> {
                    if (!firstToken.getAndSet(true)) {
                        ttft.set(System.currentTimeMillis() - startTime);
                    }
                    response.append(chunk);
                })
                .blockLast(Duration.ofSeconds(60));

        log.info("Haiku Response (Jacques Montagne persona):");
        log.info("TTFT: {}ms", ttft.get());
        log.info("Response length: {} chars", response.length());
        log.info("Response preview: {}...", response.substring(0, Math.min(200, response.length())));

        assertTrue(response.length() > 0, "Response should not be empty");
        assertTrue(ttft.get() > 0, "TTFT should be recorded");
    }

    private ModelResult testModel(ModelType modelType) {
        log.info("\n--- Testing {} ---", modelType.getDisplayName());

        InferenceRequest request = InferenceRequest.withJacquesMontagne(
                TEST_MESSAGE,
                modelType,
                DETERMINISTIC_PARAMS
        );

        StringBuilder response = new StringBuilder();
        AtomicLong ttft = new AtomicLong(0);
        AtomicBoolean firstToken = new AtomicBoolean(false);
        long startTime = System.currentTimeMillis();

        streamingClient.streamConverse(request)
                .doOnNext(chunk -> {
                    if (!firstToken.getAndSet(true)) {
                        ttft.set(System.currentTimeMillis() - startTime);
                        log.debug("First token received at {}ms", ttft.get());
                    }
                    response.append(chunk);
                })
                .blockLast(Duration.ofSeconds(60));

        long totalTime = System.currentTimeMillis() - startTime;

        return new ModelResult(
                modelType,
                response.toString(),
                ttft.get(),
                totalTime
        );
    }

    private void logComparisonResults(ModelResult claude, ModelResult llama) {
        log.info("\n========================================");
        log.info("         MODEL COMPARISON RESULTS        ");
        log.info("========================================");

        log.info("\n[{}]", claude.modelType().getDisplayName());
        log.info("  Time to First Token: {}ms", claude.ttftMs());
        log.info("  Total Generation Time: {}ms", claude.totalTimeMs());
        log.info("  Response Length: {} chars", claude.response().length());
        log.info("  Response Preview:\n  {}", truncate(claude.response(), 300));

        log.info("\n[{}]", llama.modelType().getDisplayName());
        log.info("  Time to First Token: {}ms", llama.ttftMs());
        log.info("  Total Generation Time: {}ms", llama.totalTimeMs());
        log.info("  Response Length: {} chars", llama.response().length());
        log.info("  Response Preview:\n  {}", truncate(llama.response(), 300));

        log.info("\n--- Performance Comparison ---");
        log.info("TTFT Difference: {}ms (Claude {} Haiku)",
                Math.abs(claude.ttftMs() - llama.ttftMs()),
                claude.ttftMs() < llama.ttftMs() ? "faster than" : "slower than");
        log.info("Total Time Difference: {}ms (Claude {} Haiku)",
                Math.abs(claude.totalTimeMs() - llama.totalTimeMs()),
                claude.totalTimeMs() < llama.totalTimeMs() ? "faster than" : "slower than");

        log.info("========================================\n");
    }

    private String truncate(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }

    private record ModelResult(
            ModelType modelType,
            String response,
            long ttftMs,
            long totalTimeMs
    ) {}
}
