package com.awslab.agent.controller;

import com.awslab.agent.config.AgentProperties;
import com.awslab.agent.config.BedrockConfig;
import com.awslab.agent.exception.AgentException;
import com.awslab.agent.model.AgentResponse;
import com.awslab.agent.model.ComparisonResponse;
import com.awslab.agent.service.AgentService;
import com.awslab.agent.service.ComparisonService;
import com.awslab.agent.service.ToolUseService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = AgentController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = BedrockConfig.class)
)
@ActiveProfiles("test")
class AgentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ToolUseService toolUseService;

    @MockBean
    private AgentService agentService;

    @MockBean
    private ComparisonService comparisonService;

    @MockBean
    private AgentProperties agentProperties;

    @Test
    void toolUse_validRequest_returns200() throws Exception {
        AgentResponse response = new AgentResponse("Found 3 flights", 2,
                List.of(new AgentResponse.ToolCall("searchFlights", "{}")), 3000L, null);

        when(toolUseService.execute(eq("Find flights WAW to CDG"), eq(5), eq(0.0f)))
                .thenReturn(CompletableFuture.completedFuture(response));

        MvcResult mvcResult = mockMvc.perform(post("/api/v1/agent/tool-use")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message":"Find flights WAW to CDG","maxIterations":5,"temperature":0.0}
                                """))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("Found 3 flights"))
                .andExpect(jsonPath("$.iterations").value(2))
                .andExpect(jsonPath("$.toolCalls[0].tool").value("searchFlights"));
    }

    @Test
    void toolUse_blankMessage_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/agent/tool-use")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message":"","maxIterations":5}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void toolUse_serviceFailure_returns500() throws Exception {
        when(toolUseService.execute(anyString(), anyInt(), anyFloat()))
                .thenReturn(CompletableFuture.failedFuture(
                        new AgentException(AgentException.ErrorCode.CONVERSE_API_FAILED, "API failed")));

        MvcResult mvcResult = mockMvc.perform(post("/api/v1/agent/tool-use")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message":"Find flights"}
                                """))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("CONVERSE_API_FAILED"));
    }

    @Test
    void toolUse_maxIterationsExceeded_returns422() throws Exception {
        when(toolUseService.execute(anyString(), anyInt(), anyFloat()))
                .thenReturn(CompletableFuture.failedFuture(
                        new AgentException(AgentException.ErrorCode.MAX_ITERATIONS_EXCEEDED,
                                "Exceeded maximum iterations")));

        MvcResult mvcResult = mockMvc.perform(post("/api/v1/agent/tool-use")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message":"Find flights"}
                                """))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("MAX_ITERATIONS_EXCEEDED"));
    }

    @Test
    void invokeAgent_validRequest_returns200() throws Exception {
        AgentResponse response = new AgentResponse("Found flights", 0,
                List.of(), 5000L, "session-1");

        when(agentService.invoke(eq("Find flights"), eq("session-1")))
                .thenReturn(CompletableFuture.completedFuture(response));

        MvcResult mvcResult = mockMvc.perform(post("/api/v1/agent/invoke-agent")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message":"Find flights","sessionId":"session-1"}
                                """))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("Found flights"))
                .andExpect(jsonPath("$.sessionId").value("session-1"));
    }

    @Test
    void invokeAgent_blankMessage_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/agent/invoke-agent")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message":""}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void invokeAgent_serviceFailure_returns500() throws Exception {
        when(agentService.invoke(anyString(), anyString()))
                .thenReturn(CompletableFuture.failedFuture(
                        new AgentException(AgentException.ErrorCode.AGENT_INVOCATION_FAILED, "Agent failed")));

        MvcResult mvcResult = mockMvc.perform(post("/api/v1/agent/invoke-agent")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message":"Find flights","sessionId":"session-1"}
                                """))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("AGENT_INVOCATION_FAILED"));
    }

    @Test
    void compare_validRequest_returns200() throws Exception {
        AgentResponse patternA = new AgentResponse("Answer A", 2,
                List.of(new AgentResponse.ToolCall("searchFlights", "{}")), 3000L, null);
        AgentResponse patternB = new AgentResponse("Answer B", 0,
                List.of(), 5000L, "cmp-123");
        ComparisonResponse.Analysis analysis = new ComparisonResponse.Analysis(
                2000L, 2, 1, 3000L, 5000L);
        ComparisonResponse comparison = new ComparisonResponse("Find flights", patternA, patternB, analysis);

        when(comparisonService.compare(eq("Find flights")))
                .thenReturn(CompletableFuture.completedFuture(comparison));

        MvcResult mvcResult = mockMvc.perform(post("/api/v1/agent/compare")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message":"Find flights"}
                                """))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.query").value("Find flights"))
                .andExpect(jsonPath("$.patternA.answer").value("Answer A"))
                .andExpect(jsonPath("$.patternB.answer").value("Answer B"))
                .andExpect(jsonPath("$.analysis.latencyDifferenceMs").value(2000));
    }

    @Test
    void health_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/agent/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("agent-tool-use"));
    }
}
