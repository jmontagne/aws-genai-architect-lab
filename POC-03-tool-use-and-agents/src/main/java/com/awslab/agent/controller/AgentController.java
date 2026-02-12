package com.awslab.agent.controller;

import com.awslab.agent.model.AgentRequest;
import com.awslab.agent.model.AgentResponse;
import com.awslab.agent.model.ComparisonResponse;
import com.awslab.agent.model.ToolUseRequest;
import com.awslab.agent.service.AgentService;
import com.awslab.agent.service.ComparisonService;
import com.awslab.agent.service.ToolUseService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/v1/agent")
public class AgentController {

    private static final Logger log = LoggerFactory.getLogger(AgentController.class);

    private final ToolUseService toolUseService;
    private final AgentService agentService;
    private final ComparisonService comparisonService;

    public AgentController(ToolUseService toolUseService,
                           AgentService agentService,
                           ComparisonService comparisonService) {
        this.toolUseService = toolUseService;
        this.agentService = agentService;
        this.comparisonService = comparisonService;
    }

    @PostMapping("/tool-use")
    public CompletableFuture<ResponseEntity<AgentResponse>> toolUse(
            @Valid @RequestBody ToolUseRequest request) {
        log.info("Tool-use request: message='{}'", request.message());

        return toolUseService.execute(request.message(), request.maxIterations(), request.temperature())
                .thenApply(ResponseEntity::ok);
    }

    @PostMapping("/invoke-agent")
    public CompletableFuture<ResponseEntity<AgentResponse>> invokeAgent(
            @Valid @RequestBody AgentRequest request) {
        log.info("Invoke-agent request: message='{}', sessionId='{}'",
                request.message(), request.sessionId());

        return agentService.invoke(request.message(), request.sessionId())
                .thenApply(ResponseEntity::ok);
    }

    @PostMapping("/compare")
    public CompletableFuture<ResponseEntity<ComparisonResponse>> compare(
            @Valid @RequestBody ToolUseRequest request) {
        log.info("Compare request: message='{}'", request.message());

        return comparisonService.compare(request.message())
                .thenApply(ResponseEntity::ok);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "agent-tool-use"
        ));
    }
}
