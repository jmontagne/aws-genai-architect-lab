/**
 * POC-01: Bedrock Inference and Observability Lab.
 *
 * <p>A reference implementation for deploying Amazon Bedrock inference behind
 * AWS Lambda with full observability. Demonstrates streaming vs. non-streaming
 * inference, model comparison (Claude 3.5 Sonnet vs Claude 3 Haiku), and
 * Time-To-First-Token (TTFT) measurement.</p>
 *
 * <h2>Key Patterns</h2>
 * <ul>
 *   <li><b>Streaming inference:</b> Converse API with reactive Flux bridge
 *       ({@code BedrockStreamingClient}) — measures TTFT independently of total generation time.</li>
 *   <li><b>Model comparison:</b> Side-by-side Sonnet vs Haiku with latency and token metrics.</li>
 *   <li><b>Lambda + SnapStart:</b> Spring Boot on Lambda via {@code aws-serverless-java-container},
 *       with SnapStart for sub-500ms cold starts.</li>
 *   <li><b>Observability:</b> AWS Lambda Powertools — structured JSON logging, X-Ray tracing,
 *       CloudWatch custom metrics (invocation count, TTFT, error rate).</li>
 * </ul>
 *
 * <h2>Tech Stack</h2>
 * <ul>
 *   <li>Java 21, Spring Boot 3.4, Project Reactor (Flux/Mono)</li>
 *   <li>AWS SDK v2 (BedrockRuntimeAsyncClient, Netty NIO, exponential backoff retry)</li>
 *   <li>AWS Lambda Powertools 2.8 (@Logging, @Tracing, @Metrics)</li>
 *   <li>API Gateway HTTP API v2 + Lambda SnapStart</li>
 *   <li>Terraform IaC (27 resources: API GW, Lambda, CloudWatch, S3, KMS)</li>
 * </ul>
 *
 * <h2>AIP-C01 Exam Relevance</h2>
 * <p>Covers Bedrock inference APIs, streaming vs. non-streaming trade-offs,
 * model selection criteria (cost vs. quality), Lambda deployment patterns,
 * and production observability for GenAI workloads.</p>
 *
 * @see com.jmontagne.bedrock.service.InferenceService Inference orchestration
 * @see com.jmontagne.bedrock.client.BedrockStreamingClient Reactive streaming bridge
 */
package com.jmontagne.bedrock;
