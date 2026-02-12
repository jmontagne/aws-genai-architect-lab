package com.awslab.agent.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AgentProperties {

    @Value("${aws.bedrock.model-id}")
    private String modelId;

    @Value("${aws.bedrock.agent-id}")
    private String agentId;

    @Value("${aws.bedrock.agent-alias-id}")
    private String agentAliasId;

    @Value("${aws.dynamodb.table-name}")
    private String tableName;

    @Value("${agent.tool-use.max-iterations}")
    private int maxIterations;

    @Value("${agent.tool-use.temperature}")
    private float temperature;

    @Value("${agent.tool-use.max-tokens}")
    private int maxTokens;

    public String getModelId() {
        return modelId;
    }

    public String getAgentId() {
        return agentId;
    }

    public String getAgentAliasId() {
        return agentAliasId;
    }

    public String getTableName() {
        return tableName;
    }

    public int getMaxIterations() {
        return maxIterations;
    }

    public float getTemperature() {
        return temperature;
    }

    public int getMaxTokens() {
        return maxTokens;
    }
}
