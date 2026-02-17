/**
 * POC-03: Tool Use and Bedrock Agents — Programmatic vs. Managed Orchestration.
 *
 * <p>A reference implementation comparing two patterns for giving LLMs the ability
 * to take actions (query databases, call APIs) autonomously:</p>
 *
 * <ul>
 *   <li><b>Pattern A — Programmatic Tool Use:</b> Java code controls the full ReAct loop
 *       via the Bedrock Converse API with {@code toolConfig}. Offers full transparency,
 *       Return of Control, and token-efficient orchestration (~1,900 input tokens/query).</li>
 *   <li><b>Pattern B — Managed Bedrock Agents:</b> AWS handles orchestration via
 *       InvokeAgent API with Action Groups (OpenAPI 3.0 + Lambda). Black-box execution
 *       with ~4,400 input tokens/query (~2.3x overhead from hidden system prompts).</li>
 * </ul>
 *
 * <h2>Key Findings</h2>
 * <table>
 *   <tr><th>Metric</th><th>Pattern A (Programmatic)</th><th>Pattern B (Managed)</th></tr>
 *   <tr><td>Input tokens/query</td><td>~1,900</td><td>~4,400 (2.3x)</td></tr>
 *   <tr><td>Cost @ 1M queries (Haiku)</td><td>$3,120/month</td><td>$5,520/month (+77%)</td></tr>
 *   <tr><td>Orchestration control</td><td>Full (Return of Control)</td><td>Limited (black-box)</td></tr>
 *   <tr><td>Development speed</td><td>More code</td><td>Define OpenAPI, deploy</td></tr>
 * </table>
 *
 * <h2>Tech Stack</h2>
 * <ul>
 *   <li>Java 21 with records and pattern matching</li>
 *   <li>Spring Boot 3.4 (REST API, validation, async)</li>
 *   <li>AWS SDK v2 2.29+ (BedrockRuntimeAsyncClient, BedrockAgentRuntimeAsyncClient, DynamoDbAsyncClient)</li>
 *   <li>Claude 3.5 Haiku — cost-optimized at $0.80/$4.00 per M tokens</li>
 *   <li>Amazon DynamoDB (on-demand, PAY_PER_REQUEST)</li>
 *   <li>AWS Lambda (Java 21 runtime, Action Group tool handler)</li>
 *   <li>Terraform IaC (Agent, Action Group, DynamoDB, Lambda, IAM)</li>
 * </ul>
 *
 * <h2>AIP-C01 Exam Relevance</h2>
 * <p>Covers ~15-20% of the AWS Certified Generative AI Developer Professional exam:
 * Converse API toolConfig, stopReason handling, ReAct pattern, Bedrock Agents architecture,
 * Action Groups, Return of Control, and managed vs. programmatic trade-offs.</p>
 *
 * @see com.awslab.agent.service.ToolUseService Pattern A implementation
 * @see com.awslab.agent.service.AgentService Pattern B implementation
 * @see com.awslab.agent.service.ComparisonService Empirical comparison
 */
package com.awslab.agent;
