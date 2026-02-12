package com.awslab.agent.service;

import com.awslab.agent.config.AgentProperties;
import com.awslab.agent.model.AgentResponse;
import com.awslab.agent.model.ComparisonResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class ComparisonService {

    private static final Logger log = LoggerFactory.getLogger(ComparisonService.class);

    private final ToolUseService toolUseService;
    private final AgentService agentService;
    private final AgentProperties properties;

    public ComparisonService(ToolUseService toolUseService,
                             AgentService agentService,
                             AgentProperties properties) {
        this.toolUseService = toolUseService;
        this.agentService = agentService;
        this.properties = properties;
    }

    public CompletableFuture<ComparisonResponse> compare(String message) {
        log.info("Running comparison for: {}", message);

        String sessionId = "cmp-" + UUID.randomUUID();

        CompletableFuture<AgentResponse> patternA = toolUseService.execute(
                message, properties.getMaxIterations(), properties.getTemperature());

        CompletableFuture<AgentResponse> patternB = agentService.invoke(message, sessionId);

        return patternA.thenCombine(patternB, (responseA, responseB) -> {
            long latencyDiff = Math.abs(responseA.latencyMs() - responseB.latencyMs());

            ComparisonResponse.Analysis analysis = new ComparisonResponse.Analysis(
                    latencyDiff,
                    responseA.iterations(),
                    responseA.toolCalls().size(),
                    responseA.latencyMs(),
                    responseB.latencyMs()
            );

            log.info("Comparison complete: patternA={}ms, patternB={}ms, diff={}ms",
                    responseA.latencyMs(), responseB.latencyMs(), latencyDiff);

            return new ComparisonResponse(message, responseA, responseB, analysis);
        });
    }
}
