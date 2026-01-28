# POC-01: Bedrock Inference & Observability Lab

## Overview

This Proof of Concept demonstrates **enterprise-grade integration with Amazon Bedrock**, AWS's fully managed service for foundation models. It showcases how to build production-ready GenAI applications with proper observability, error handling, and streaming capabilities.

**Why this POC matters:** In enterprise environments, simply calling an LLM API is not enough. Organizations need audit trails, cost tracking, graceful error handling, and performance monitoring. This POC addresses all these concerns while maintaining clean, maintainable code.

> 💡 **For Junior Developers:** This README is designed to teach you not just *how* to use these technologies, but *why* they exist and *when* to use them. Take your time reading each section!

---

## Table of Contents

1. [Glossary for Beginners](#glossary-for-beginners)
2. [Architecture](#architecture)
3. [Technology Stack](#technology-stack)
4. [Understanding the Core Concepts](#understanding-the-core-concepts)
5. [Key Concepts Explained](#key-concepts-explained)
6. [Prerequisites](#prerequisites)
7. [Installation](#installation)
8. [Project Structure](#project-structure)
9. [How It Works](#how-it-works)
10. [API Reference](#api-reference)
11. [Testing](#testing)
12. [Infrastructure as Code](#infrastructure-as-code)
13. [Learning Outcomes](#learning-outcomes)
14. [Further Reading](#further-reading)

---

## Glossary for Beginners

Before diving in, let's define some terms you'll encounter:

| Term | Simple Definition |
|------|-------------------|
| **LLM** | Large Language Model - AI that understands and generates human-like text (like ChatGPT, Claude) |
| **Token** | A piece of text (roughly 4 characters or 0.75 words). LLMs process text in tokens. |
| **Inference** | The process of getting a response from an AI model (asking it a question) |
| **Streaming** | Receiving data piece by piece instead of waiting for the entire response |
| **API** | Application Programming Interface - a way for programs to talk to each other |
| **SDK** | Software Development Kit - pre-built code that makes it easier to use an API |
| **Reactive Programming** | A programming style where you work with streams of data that arrive over time |
| **Serverless** | Running code without managing servers - you just deploy code, AWS handles the rest |
| **IaC** | Infrastructure as Code - defining your cloud resources in text files (like Terraform) |

---

## Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              AWS Cloud                                       │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                        Amazon Bedrock                                │   │
│  │  ┌─────────────────┐     ┌─────────────────┐                        │   │
│  │  │ Claude 3.5      │     │ Claude 3 Haiku  │                        │   │
│  │  │ Sonnet          │     │ (Fast/Cheap)    │                        │   │
│  │  └────────┬────────┘     └────────┬────────┘                        │   │
│  │           │     Converse Stream API     │                            │   │
│  │           └──────────────┬──────────────┘                            │   │
│  └──────────────────────────┼──────────────────────────────────────────┘   │
│                             │                                               │
│  ┌──────────────────────────┼──────────────────────────────────────────┐   │
│  │              Model Invocation Logging                                │   │
│  │  ┌─────────────────┐     │     ┌─────────────────┐                  │   │
│  │  │ CloudWatch Logs │◄────┴────►│ S3 Bucket       │                  │   │
│  │  │ (Real-time)     │           │ (Long-term)     │                  │   │
│  │  └─────────────────┘           └─────────────────┘                  │   │
│  │           │                            │                             │   │
│  │           └────────────┬───────────────┘                             │   │
│  │                   KMS Encryption                                     │   │
│  └──────────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────────┘
                                    │
                                    │ HTTPS
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                         Spring Boot Application                              │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐  │
│  │ Controller  │───►│ Service     │───►│ Streaming   │───►│ Async       │  │
│  │ (WebFlux)   │    │ Layer       │    │ Client      │    │ Bedrock     │  │
│  │             │◄───│             │◄───│ (Flux)      │◄───│ Client      │  │
│  └─────────────┘    └─────────────┘    └─────────────┘    └─────────────┘  │
│        │                                                                     │
│        │ Server-Sent Events (SSE)                                           │
│        ▼                                                                     │
│  ┌─────────────┐                                                            │
│  │ Client      │  curl -N http://localhost:8080/api/v1/inference/stream/... │
│  └─────────────┘                                                            │
└─────────────────────────────────────────────────────────────────────────────┘
```

> **Note:** The above diagram shows local development mode. In production, the application runs on **AWS Lambda** with **API Gateway HTTP API**. See [Lambda Deployment](#lambda-deployment) for details.

---

## Lambda Deployment

This POC is deployed as a **serverless application** on AWS Lambda with the following stack:

### Why Lambda for Java?

| Challenge | Solution |
|-----------|----------|
| Java cold starts are slow (~5-10s) | **SnapStart** pre-initializes the JVM, reducing cold start to ~200ms |
| Spring Boot is heavyweight | **Lazy initialization** defers bean creation until needed |
| Lambda doesn't support SSE streaming | **Buffered responses** collect all tokens and return complete JSON |
| WebFlux doesn't work with Lambda adapter | **Blocking calls** using `.block()` for servlet compatibility |

### Architecture Components

| Component | Purpose |
|-----------|---------|
| **API Gateway HTTP API** | Routes HTTPS requests to Lambda, handles CORS |
| **Lambda Function** | Runs Spring Boot with `aws-serverless-java-container` |
| **Lambda Alias (live)** | Points to published version with SnapStart |
| **CloudWatch Logs** | Captures application and access logs |
| **X-Ray** | Distributed tracing (with Powertools integration) |

### Key Implementation Details

**1. Servlet Adapter (not WebFlux)**

The `aws-serverless-java-container-springboot3` library requires servlet mode:

```yaml
# application-lambda.yml
spring:
  main:
    web-application-type: servlet  # Forces servlet mode
```

**2. Blocking Controller Calls**

Reactive types don't work through the Lambda servlet adapter, so we convert to blocking:

```java
// Before (doesn't work on Lambda)
@GetMapping("/health")
public Mono<String> health() {
    return Mono.just("OK");
}

// After (works on Lambda)
@GetMapping("/health")
public HealthResponse health() {
    return new HealthResponse("OK", "bedrock-inference-lab");
}
```

**3. SnapStart Configuration**

Terraform configures SnapStart for fast cold starts:

```hcl
snap_start {
  apply_on = "PublishedVersions"
}
```

### Testing the Deployed API

After `terraform apply`, you'll get outputs like:

```
api_gateway_url = "https://xxxxx.execute-api.us-east-1.amazonaws.com"
```

Test with curl:

```bash
# Health check
curl "https://xxxxx.execute-api.us-east-1.amazonaws.com/api/v1/inference/health"

# List models
curl "https://xxxxx.execute-api.us-east-1.amazonaws.com/api/v1/inference/models"

# Inference with Claude 3 Haiku
curl "https://xxxxx.execute-api.us-east-1.amazonaws.com/api/v1/inference/stream/CLAUDE_3_HAIKU?message=Hello"

# Inference with Claude 3.5 Sonnet
curl "https://xxxxx.execute-api.us-east-1.amazonaws.com/api/v1/inference/stream/CLAUDE_3_5_SONNET?message=Tell%20me%20a%20joke"
```

---

## Technology Stack

### Why These Technologies?

| Technology | Version | Purpose | Why This Choice? | Learn More |
|------------|---------|---------|------------------|------------|
| **Java** | 21 | Application runtime | LTS version with modern features (records, pattern matching). Enterprise standard. | [OpenJDK 21](https://openjdk.org/projects/jdk/21/) |
| **Spring Boot** | 3.4.1 | Web framework | Servlet mode for Lambda, WebFlux for local dev. Industry standard. | [Spring Boot Docs](https://docs.spring.io/spring-boot/docs/current/reference/html/) |
| **AWS SDK v2** | 2.29.0 | AWS integration | Async client support, better performance than SDK v1. | [AWS SDK for Java v2](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/home.html) |
| **Project Reactor** | 3.6.11 | Reactive streams | Used internally for async Bedrock calls; converted to blocking for Lambda. | [Project Reactor](https://projectreactor.io/docs/core/release/reference/) |
| **LangChain4j** | 1.0.0-beta1 | LLM abstraction | Provides higher-level abstractions for LLM operations. | [LangChain4j GitHub](https://github.com/langchain4j/langchain4j) |
| **Terraform** | >= 1.5 | Infrastructure as Code | Declarative, reproducible infrastructure. Industry standard. | [Terraform Docs](https://developer.hashicorp.com/terraform/docs) |

### Why Amazon Bedrock?

[Amazon Bedrock](https://aws.amazon.com/bedrock/) provides:
- **No infrastructure management** - Fully managed, serverless
- **Multiple model providers** - Anthropic, Meta, Cohere, Amazon, etc.
- **Enterprise features** - VPC endpoints, IAM integration, logging
- **Unified API** - Same `Converse` API works across all models

---

## Understanding the Core Concepts

> 💡 **This section explains the "big picture" concepts. Read this before diving into code!**

### What is Amazon Bedrock?

**Amazon Bedrock** is AWS's managed service for accessing Large Language Models (LLMs). Think of it as a "vending machine for AI" - you don't need to know how to train models or manage GPU servers. You just:

1. Choose a model (Claude, Llama, etc.)
2. Send your prompt
3. Get a response
4. Pay only for what you use

```
┌─────────────────────────────────────────────────────────────┐
│                    Amazon Bedrock                            │
│                                                              │
│   ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐       │
│   │ Claude  │  │ Llama   │  │ Titan   │  │ Cohere  │       │
│   │(Anthrop)│  │ (Meta)  │  │ (AWS)   │  │         │       │
│   └─────────┘  └─────────┘  └─────────┘  └─────────┘       │
│                                                              │
│              ▲ All accessed via ONE unified API ▲           │
└─────────────────────────────────────────────────────────────┘
                              │
                    Your Application
```

**Why not just use OpenAI directly?**
- Bedrock integrates with AWS security (IAM, VPC, encryption)
- One API to switch between multiple models
- Built-in logging and compliance features
- Data stays within your AWS account

📚 [Amazon Bedrock Documentation](https://docs.aws.amazon.com/bedrock/latest/userguide/what-is-bedrock.html)

---

### What is the Converse API?

The **Converse API** is Amazon Bedrock's unified interface for chatting with any supported model. Before Converse, you had to learn different request formats for each model provider.

**The Problem (Before Converse):**
```java
// For Claude - one format
String claudePayload = "{\"prompt\": \"Human: Hello\\n\\nAssistant:\"}";

// For Llama - different format!
String llamaPayload = "{\"prompt\": \"<s>[INST] Hello [/INST]\"}";

// For Titan - yet another format!
String titanPayload = "{\"inputText\": \"Hello\"}";
```

**The Solution (Converse API):**
```java
// Same format for ALL models!
ConverseRequest request = ConverseRequest.builder()
    .modelId("anthropic.claude-3-5-sonnet...")  // Or any other model
    .messages(Message.builder()
        .role(ConversationRole.USER)
        .content(ContentBlock.fromText("Hello"))
        .build())
    .build();
```

**Converse vs ConverseStream:**
| Method | Behavior | Use Case |
|--------|----------|----------|
| `converse()` | Waits for complete response | Simple requests, batch processing |
| `converseStream()` | Returns tokens as they're generated | Chat UIs, real-time applications |

📚 [Converse API Documentation](https://docs.aws.amazon.com/bedrock/latest/userguide/conversation-inference.html)

---

### What is LangChain4j?

[LangChain4j](https://github.com/langchain4j/langchain4j) is the Java version of the popular [LangChain](https://www.langchain.com/) framework. It provides **high-level abstractions** for working with LLMs.

**Without LangChain4j (Low-level):**
```java
// You handle everything manually
BedrockRuntimeAsyncClient client = BedrockRuntimeAsyncClient.create();
ConverseRequest request = ConverseRequest.builder()
    .modelId("anthropic.claude-3-5-sonnet-20240620-v1:0")
    .messages(...)
    .system(...)
    .inferenceConfig(...)
    .build();
CompletableFuture<ConverseResponse> response = client.converse(request);
// Parse response, handle errors, manage context...
```

**With LangChain4j (High-level):**
```java
// LangChain4j abstracts the complexity
ChatLanguageModel model = BedrockChatModel.builder()
    .modelId("anthropic.claude-3-5-sonnet-20240620-v1:0")
    .build();

String response = model.generate("Hello!"); // That's it!
```

**When to use which?**
| Approach | Use When |
|----------|----------|
| Low-level (AWS SDK) | You need full control, custom streaming, performance tuning |
| High-level (LangChain4j) | Rapid prototyping, simple use cases, RAG pipelines |

> 💡 **This POC uses the low-level AWS SDK** to teach you how streaming actually works. Once you understand it, LangChain4j will feel like magic!

📚 [LangChain4j Documentation](https://docs.langchain4j.dev/)

---

### What is Reactive Programming? (Project Reactor)

**Traditional (Blocking) Programming:**
```java
// Thread waits (blocked) until response arrives
String response = callApi();  // ⏳ Thread stuck here for 5 seconds
System.out.println(response);
```

**Reactive (Non-Blocking) Programming:**
```java
// Thread continues, data arrives over time
Flux<String> stream = callApiStream();
stream.subscribe(chunk -> System.out.print(chunk));  // Prints as data arrives
// Thread is free to do other work!
```

**Key Concepts:**

| Concept | What It Is | Analogy |
|---------|------------|---------|
| `Mono<T>` | A container for 0 or 1 item that arrives later | A promise for a single value |
| `Flux<T>` | A container for 0 to N items that arrive over time | A promise for a stream of values |
| `subscribe()` | "Start listening" for data | Turning on the faucet |
| `Sink` | A way to push data into a Flux | The source of the water |

**Why use Reactive for LLM streaming?**
```
Traditional: Wait 5 sec → Get entire response → Show to user
Reactive:    Token 1 → Token 2 → Token 3 → ... (user sees immediately)
```

📚 [Project Reactor Reference Guide](https://projectreactor.io/docs/core/release/reference/)

---

### How Does Java Run "Serverless" with Bedrock?

> 💡 **Common confusion:** "Serverless" doesn't mean "no servers." It means "no servers for YOU to manage."

**This POC runs in two modes:**

#### Mode 1: Local Development (This POC)
```
Your Machine                          AWS Cloud
┌─────────────┐                      ┌─────────────────┐
│ Spring Boot │  ───HTTPS───────►   │ Amazon Bedrock  │
│ Application │                      │ (Serverless)    │
└─────────────┘                      └─────────────────┘
   You manage                         AWS manages
```

#### Mode 2: Production Serverless (Future POCs)
```
AWS Cloud
┌─────────────────────────────────────────────────────────────┐
│  ┌─────────────┐                    ┌─────────────────┐    │
│  │ AWS Lambda  │  ───────────────►  │ Amazon Bedrock  │    │
│  │ (Your code) │                    │                 │    │
│  └─────────────┘                    └─────────────────┘    │
│     AWS manages                        AWS manages          │
└─────────────────────────────────────────────────────────────┘
```

**Java on AWS Lambda (Serverless):**

Java can run on [AWS Lambda](https://aws.amazon.com/lambda/) using:
- **Standard JAR** - Simple but slower cold starts (~3-5 seconds)
- **GraalVM Native Image** - Compiles Java to native binary, fast cold starts (~200ms)
- **SnapStart** - AWS pre-warms your Lambda, instant starts

```java
// AWS Lambda Handler in Java
public class BedrockHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final BedrockRuntimeClient client = BedrockRuntimeClient.create();

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        // Your Bedrock call here
        return new APIGatewayProxyResponseEvent()
            .withStatusCode(200)
            .withBody(response);
    }
}
```

📚 [AWS Lambda with Java](https://docs.aws.amazon.com/lambda/latest/dg/lambda-java.html)
📚 [Lambda SnapStart](https://docs.aws.amazon.com/lambda/latest/dg/snapstart.html)

---

### What is Spring WebFlux?

[Spring WebFlux](https://docs.spring.io/spring-framework/reference/web/webflux.html) is Spring's reactive web framework. It's an alternative to Spring MVC for building non-blocking web applications.

> ⚠️ **Lambda Compatibility:** WebFlux does NOT work with `aws-serverless-java-container-springboot3`. This POC removed the WebFlux dependency for Lambda deployment and uses blocking calls instead. The concepts below apply to local development only.

**Spring MVC (Traditional) - Used for Lambda:**
```java
@GetMapping("/chat")
public String chat(@RequestParam String message) {
    return bedrockService.chat(message);  // Thread blocked until response
}
```

**Spring WebFlux (Reactive) - Local dev only:**
```java
@GetMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<String> chatStream(@RequestParam String message) {
    return bedrockService.streamChat(message);  // Streams tokens as they arrive
}
```

**When to use which?**
| Framework | Use When |
|-----------|----------|
| Spring MVC (Servlet) | Lambda deployment, traditional CRUD apps |
| Spring WebFlux | Local dev streaming, high concurrency (NOT Lambda) |

📚 [Spring WebFlux Documentation](https://docs.spring.io/spring-framework/reference/web/webflux.html)

---

### What are Server-Sent Events (SSE)?

**SSE** is a web standard for servers to push data to browsers over HTTP. It's simpler than WebSockets for one-way streaming.

```
Browser                              Server
   │                                    │
   │  GET /stream                       │
   │ ──────────────────────────────────►│
   │                                    │
   │  HTTP 200 OK                       │
   │  Content-Type: text/event-stream   │
   │◄──────────────────────────────────│
   │                                    │
   │  data: Hello                       │
   │◄──────────────────────────────────│
   │                                    │
   │  data: , how                       │
   │◄──────────────────────────────────│
   │                                    │
   │  data: are you?                    │
   │◄──────────────────────────────────│
```

**In this POC:**
```java
@GetMapping(value = "/stream/{model}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<String> stream(...) {
    // MediaType.TEXT_EVENT_STREAM_VALUE = "text/event-stream"
    // This tells Spring to format responses as SSE
}
```

📚 [MDN: Server-Sent Events](https://developer.mozilla.org/en-US/docs/Web/API/Server-sent_events)

---

## Key Concepts Explained

> 💡 **This section dives deeper into specific implementation details.**

### 1. The Converse API vs InvokeModel

Amazon Bedrock offers two ways to call models:

| API | Use Case | Pros | Cons |
|-----|----------|------|------|
| [`InvokeModel`](https://docs.aws.amazon.com/bedrock/latest/APIReference/API_runtime_InvokeModel.html) | Legacy, model-specific | Full control over request format | Different payload for each model |
| [`Converse`](https://docs.aws.amazon.com/bedrock/latest/APIReference/API_runtime_Converse.html) | Recommended, unified | Same request format for all models | Slightly less control |

**This POC uses `ConverseStream`** - the streaming version of the Converse API. This provides:
- Token-by-token response delivery
- Lower perceived latency (user sees output immediately)
- Unified interface across Claude, Llama, etc.

📚 [Converse API Reference](https://docs.aws.amazon.com/bedrock/latest/userguide/conversation-inference.html)

### 2. Streaming with Project Reactor

> 💡 **Why streaming matters:** Users prefer seeing responses appear word-by-word rather than waiting for a complete response. This is called "perceived performance."

> ⚠️ **Lambda Limitation:** True SSE streaming is NOT supported through API Gateway + Lambda. This POC uses **buffered responses** on Lambda - all tokens are collected internally, then returned as complete JSON. For true streaming, use Lambda Response Streaming or run locally.

**How it works internally:**

The `BedrockStreamingClient` still uses reactive streaming to receive tokens from Bedrock:

```java
// Internal: Bedrock streams tokens via Flux
Flux<String> stream = bedrockClient.converseStream(request);

// Lambda mode: Collect all tokens and return as JSON
List<String> chunks = stream.collectList().block(timeout);
String fullResponse = String.join("", chunks);
return new StreamResponse(fullResponse, model, chunks.size());
```

**Visual comparison:**

```
Local Development (WebFlux + SSE):
[Start]──[H]──[ello]──[, ]──[how]──[are]──[you]──[?]
          │     │      │     │      │      │      │
          ▼     ▼      ▼     ▼      ▼      ▼      ▼
         User sees each chunk immediately (Server-Sent Events)

Lambda Deployment (Buffered JSON):
[Start]────────────────────────────────────[Complete JSON Response]
        Tokens collected internally               │
                                                  ▼
                            {"content":"Hello, how are you?","chunksReceived":7}
```

📚 [Reactor Flux Documentation](https://projectreactor.io/docs/core/release/api/reactor/core/publisher/Flux.html)

### 3. The System Prompt Pattern ("Jacques Montagne")

A **system prompt** defines the AI's persona and behavior. This POC uses the "Jacques Montagne" pattern:

```
You are Jacques Montagne, a distinguished French master chef...
```

**Why this matters:**
- **Consistency** - Every response follows the same persona
- **Control** - Limits the AI to a specific domain (cooking)
- **Safety** - Prevents off-topic or harmful responses
- **Testing** - Same prompt across models enables fair comparison

> 💡 **Prompt Engineering Tip:** System prompts are your first line of defense for controlling AI behavior. They're set once and apply to the entire conversation.

**Message roles in the Converse API:**

| Role | Purpose | Example |
|------|---------|---------|
| `system` | Sets AI behavior/persona | "You are a helpful cooking assistant..." |
| `user` | Human's message | "How do I make a roux?" |
| `assistant` | AI's response | "A roux is made by..." |

📚 [Anthropic Prompt Engineering Guide](https://docs.anthropic.com/en/docs/build-with-claude/prompt-engineering/overview)

### 4. Inference Parameters

These parameters control the model's output:

| Parameter | Range | Effect | Use Case |
|-----------|-------|--------|----------|
| `temperature` | 0.0 - 1.0 | Randomness/creativity | 0 = deterministic, 1 = creative |
| `topP` | 0.0 - 1.0 | Nucleus sampling | Controls diversity of word choices |
| `maxTokens` | 1 - 4096+ | Response length limit | Prevents runaway responses |
| `stopSequences` | strings | Early termination | Stop at specific patterns |

**For reproducible results** (testing, compliance), use `temperature=0`.

> 💡 **Understanding Temperature:**
> - `temperature=0`: Model always picks the most likely next word (deterministic)
> - `temperature=0.7`: Good balance for creative writing
> - `temperature=1.0`: Maximum creativity, but may produce nonsense

**Visual explanation of Temperature:**

```
Prompt: "The capital of France is"

temperature=0 → "Paris" (always)
temperature=0.5 → "Paris" (usually), "the city of Paris" (sometimes)
temperature=1.0 → "Paris", "a beautiful city", "where I lived" (varied)
```

📚 [LLM Parameters Explained](https://docs.aws.amazon.com/bedrock/latest/userguide/inference-parameters.html)

### 5. Model Invocation Logging

Every Bedrock call can be logged for:
- **Compliance** - Audit trail of all AI interactions
- **Debugging** - See exact prompts and responses
- **Cost tracking** - Monitor token usage
- **Security** - Detect misuse or prompt injection

This POC configures logging to both:
- **[CloudWatch Logs](https://aws.amazon.com/cloudwatch/)** - Real-time access, 30-day retention
- **[S3 Bucket](https://aws.amazon.com/s3/)** - Long-term storage, encrypted, lifecycle policies

> 💡 **Why log everything?** In enterprise environments, you need to answer questions like:
> - "What did the AI say to this customer on March 15th?"
> - "How much are we spending on tokens per department?"
> - "Did anyone try to misuse the AI system?"

**Example log entry:**
```json
{
  "timestamp": "2024-01-15T10:30:00Z",
  "modelId": "anthropic.claude-3-5-sonnet-20240620-v1:0",
  "inputTokens": 150,
  "outputTokens": 450,
  "latencyMs": 2340,
  "input": { "messages": [...] },
  "output": { "content": [...] }
}
```

📚 [Bedrock Model Invocation Logging](https://docs.aws.amazon.com/bedrock/latest/userguide/model-invocation-logging.html)

---

## Prerequisites

> 💡 **New to AWS?** Don't worry! This section walks you through everything you need.

### AWS Account Setup

1. **AWS Account** with Bedrock access
   - [Create an AWS Account](https://portal.aws.amazon.com/billing/signup) (free tier available)
   - Bedrock is available in specific regions: `us-east-1`, `us-west-2`, `eu-west-1`, etc.

2. **Model Access** - Enable in AWS Console:
   - Go to [Amazon Bedrock Console](https://console.aws.amazon.com/bedrock/)
   - Click "Model access" in the left sidebar
   - Request access to "Claude 3.5 Sonnet" and "Claude 3 Haiku"
   - Wait for approval (usually instant for Claude models)

   > ⚠️ **Important:** You must explicitly request model access. Without this, API calls will fail with `AccessDeniedException`.

3. **AWS Credentials** configured locally:
   ```bash
   # Option 1: AWS CLI configure (recommended for beginners)
   aws configure
   # Enter your Access Key ID, Secret Access Key, and region

   # Option 2: Environment variables
   export AWS_ACCESS_KEY_ID=your_key
   export AWS_SECRET_ACCESS_KEY=your_secret
   export AWS_REGION=us-east-1
   ```

   📚 [Setting up AWS Credentials](https://docs.aws.amazon.com/cli/latest/userguide/cli-configure-files.html)

### Development Tools

| Tool | Version | Installation | Verify |
|------|---------|--------------|--------|
| Java | 21+ | [SDKMAN](https://sdkman.io/): `sdk install java 21-open` | `java -version` |
| Maven | 3.9+ | [SDKMAN](https://sdkman.io/): `sdk install maven` | `mvn -version` |
| Terraform | 1.5+ | [HashiCorp](https://developer.hashicorp.com/terraform/install): `brew install terraform` | `terraform -version` |
| AWS CLI | 2.x | [AWS](https://docs.aws.amazon.com/cli/latest/userguide/getting-started-install.html): `brew install awscli` | `aws --version` |

> 💡 **Tip for beginners:** [SDKMAN](https://sdkman.io/) makes it easy to install and switch between Java/Maven versions:
> ```bash
> # Install SDKMAN (Linux/macOS/WSL)
> curl -s "https://get.sdkman.io" | bash
>
> # Then install Java and Maven
> sdk install java 21-open
> sdk install maven
> ```

---

## Installation

### Step 1: Clone and Navigate

```bash
cd POC-01-bedrock-Inference-and-observability-lab
```

### Step 2: Build the Application

```bash
# Compile and download dependencies
mvn clean compile

# Run tests (requires AWS credentials + model access)
mvn test

# Package as JAR (creates shaded uber-JAR for Lambda)
mvn package -DskipTests
```

### Step 3: Deploy to AWS Lambda

The Terraform configuration creates the complete serverless stack:

```bash
cd terraform

# Initialize Terraform
terraform init

# Preview changes
terraform plan

# Deploy (requires AWS credentials)
terraform apply
```

**What gets created (27 resources):**
- **Lambda Function** - Java 21 with SnapStart enabled
- **Lambda Alias** - Points to published version for SnapStart
- **API Gateway HTTP API** - Routes requests to Lambda
- **IAM Roles** - Lambda execution role with Bedrock permissions
- **CloudWatch Log Groups** - For Lambda and API Gateway
- **KMS Key** - For encryption at rest
- **S3 Bucket** - Long-term log storage with lifecycle policies
- **Bedrock Logging** - Model invocation logging configuration

**Terraform Outputs:**

```bash
terraform output
# api_gateway_url = "https://xxxxx.execute-api.us-east-1.amazonaws.com"
# api_health_endpoint = "https://xxxxx.execute-api.us-east-1.amazonaws.com/api/v1/inference/health"
# lambda_function_name = "bedrock-inference-lab-dev"
```

### Step 4: Test the Deployed API

```bash
# Get the API URL
export API_URL=$(terraform output -raw api_gateway_url)

# Test health endpoint
curl "$API_URL/api/v1/inference/health"
# {"status":"OK","service":"bedrock-inference-lab"}

# Test inference
curl "$API_URL/api/v1/inference/stream/CLAUDE_3_HAIKU?message=Hello"
```

### (Optional) Local Development

For local development without Lambda:

```bash
cd ..  # Back to POC root

# Run locally with Spring Boot
mvn spring-boot:run

# Application starts on http://localhost:8080
```

> **Note:** Local mode uses WebFlux streaming (SSE). Lambda mode uses buffered JSON responses.

---

## Project Structure

```
POC-01-bedrock-Inference-and-observability-lab/
│
├── pom.xml                          # Maven configuration
│
├── terraform/                       # Infrastructure as Code
│   ├── main.tf                      # Root module
│   ├── variables.tf                 # Input variables
│   ├── outputs.tf                   # Output values
│   ├── versions.tf                  # Provider versions
│   └── modules/
│       └── bedrock-logging/         # Reusable logging module
│           ├── main.tf              # Logging configuration resource
│           ├── iam.tf               # IAM role for Bedrock
│           ├── s3.tf                # Encrypted S3 bucket
│           ├── cloudwatch.tf        # Log group
│           ├── variables.tf
│           └── outputs.tf
│
├── src/main/java/com/jmontagne/bedrock/
│   │
│   ├── BedrockInferenceApplication.java   # Spring Boot entry point
│   │
│   ├── config/
│   │   └── BedrockClientConfig.java       # AWS client configuration
│   │       # - Async client setup
│   │       # - Retry policy with exponential backoff
│   │       # - Connection/read timeouts
│   │
│   ├── client/
│   │   └── BedrockStreamingClient.java    # Core streaming logic
│   │       # - Converts AWS callbacks to Reactor Flux
│   │       # - Tracks Time To First Token (TTFT)
│   │       # - Handles streaming events
│   │
│   ├── service/
│   │   └── InferenceService.java          # Business logic
│   │       # - Jacques Montagne persona
│   │       # - Model comparison
│   │
│   ├── controller/
│   │   └── InferenceController.java       # REST endpoints
│   │       # - Streaming endpoints (SSE)
│   │       # - Non-streaming endpoints
│   │       # - Model listing
│   │
│   ├── model/
│   │   ├── ModelType.java                 # Supported models enum
│   │   ├── InferenceParameters.java       # temp, topP, maxTokens
│   │   ├── InferenceRequest.java          # Request wrapper
│   │   ├── InferenceResponse.java         # Response wrapper
│   │   └── PerformanceMetrics.java        # TTFT, latency tracking
│   │
│   └── exception/
│       └── BedrockExceptionHandler.java   # Global error handling
│           # - ThrottlingException → 429
│           # - ModelNotReadyException → 503
│           # - Retry-After headers
│
├── src/main/resources/
│   └── application.yml                    # Spring configuration
│
└── src/test/java/com/jmontagne/bedrock/comparison/
    └── ModelComparisonTest.java           # Integration tests
```

---

## How It Works

> 💡 **For Junior Developers:** This section shows the complete journey of a request through the application. Understanding this flow is crucial for debugging and extending the code.

### The Streaming Flow

```
1. HTTP Request arrives
   └─► InferenceController receives GET /api/v1/inference/stream/CLAUDE_3_5_SONNET?message=Hello

2. Controller calls Service
   └─► InferenceService.streamWithJacquesMontagne(message, model, params)

3. Service creates Request
   └─► InferenceRequest with system prompt + user message + parameters

4. Client initiates streaming
   └─► BedrockStreamingClient.streamConverse(request)
       │
       ├─► Creates Reactor Sink (Sinks.Many<String>)
       │   This is a "bridge" between AWS callbacks and reactive streams
       │
       ├─► Builds ConverseStreamRequest with:
       │   - modelId: "anthropic.claude-3-5-sonnet-20240620-v1:0"
       │   - system: [Jacques Montagne prompt]
       │   - messages: [{role: USER, content: "Hello"}]
       │   - inferenceConfig: {temperature, topP, maxTokens}
       │
       └─► Calls bedrockClient.converseStream() with ResponseHandler

5. AWS sends streaming events
   └─► ContentBlockDeltaEvent arrives with text chunk
       │
       ├─► First chunk? Record Time To First Token (TTFT)
       │
       └─► sink.tryEmitNext(chunk) → pushes to Flux

6. Flux flows back through layers
   └─► Controller returns Flux<String> with MediaType.TEXT_EVENT_STREAM_VALUE
       │
       └─► Spring WebFlux converts to Server-Sent Events:
           data:Hello
           data:, how
           data: are
           data: you
           data:?
```

### Error Handling Strategy

> 💡 **Why proper error handling matters:** In production, things fail. Networks drop, services throttle, models cold-start. Your application must handle these gracefully.

```java
// In BedrockExceptionHandler.java

ThrottlingException → HTTP 429 + Retry-After: 5
  "You're calling too fast. Wait and retry."

ModelNotReadyException → HTTP 503 + Retry-After: 30
  "Model is cold-starting. Wait and retry."

ModelTimeoutException → HTTP 504
  "Request took too long. Try shorter input."

ValidationException → HTTP 400
  "Bad request parameters."

AccessDeniedException → HTTP 403
  "Check IAM permissions and model access."
```

**HTTP Status Code Reference:**

| Code | Meaning | Client Action |
|------|---------|---------------|
| 200 | Success | Process response |
| 400 | Bad Request | Fix request parameters |
| 403 | Forbidden | Check IAM permissions |
| 429 | Too Many Requests | Wait and retry |
| 503 | Service Unavailable | Wait and retry |
| 504 | Gateway Timeout | Retry with shorter input |

📚 [HTTP Status Codes](https://developer.mozilla.org/en-US/docs/Web/HTTP/Status)

### The Retry Policy

> 💡 **Why retries with backoff?** When a service is overloaded, immediately retrying makes things worse. "Exponential backoff with jitter" spaces out retries randomly to avoid thundering herds.

```java
RetryPolicy.builder(RetryMode.ADAPTIVE)
    .numRetries(3)                          // Try 3 times
    .backoffStrategy(FullJitterBackoffStrategy.builder()
        .baseDelay(Duration.ofMillis(100))  // Start at 100ms
        .maxBackoffTime(Duration.ofSeconds(20))  // Cap at 20s
        .build())
    .build();

// What this does:
// Attempt 1: Fails → wait random(0-100ms)
// Attempt 2: Fails → wait random(0-200ms)
// Attempt 3: Fails → wait random(0-400ms)
// (capped at 20 seconds maximum)
```

**Adaptive retry** automatically adjusts based on:
- Throttling responses from AWS
- Service health indicators
- Previous retry outcomes

---

## API Reference

### Base URL

After deploying with Terraform, get your API URL from the outputs:

```bash
terraform output api_gateway_url
# Example: https://xxxxx.execute-api.us-east-1.amazonaws.com
```

### Available Endpoints

| Method | Endpoint | Description | Response |
|--------|----------|-------------|----------|
| GET | `/api/v1/inference/health` | Health check | `{"status":"OK","service":"bedrock-inference-lab"}` |
| GET | `/api/v1/inference/models` | List available models | JSON array of model info |
| GET | `/api/v1/inference/stream/{model}` | Inference (buffered) | JSON with content and metrics |
| POST | `/api/v1/inference/stream/{model}` | Inference with custom prompt | JSON with content and metrics |
| GET | `/api/v1/inference/{model}` | Non-streaming inference | JSON with response and metrics |
| GET | `/api/v1/inference/compare` | Compare two models | Text comparison |

### Model Types

| Model | Enum Value | Use Case |
|-------|------------|----------|
| Claude 3.5 Sonnet | `CLAUDE_3_5_SONNET` | High quality, complex tasks |
| Claude 3 Haiku | `CLAUDE_3_HAIKU` | Fast, cost-effective |

### Inference Request

```bash
# Basic inference with Claude 3 Haiku (fast)
curl "https://YOUR_API_ID.execute-api.us-east-1.amazonaws.com/api/v1/inference/stream/CLAUDE_3_HAIKU?message=Hello"

# With parameters
curl "https://YOUR_API_ID.execute-api.us-east-1.amazonaws.com/api/v1/inference/stream/CLAUDE_3_5_SONNET?\
message=What%20is%20a%20roux?&\
temperature=0&\
maxTokens=500"

# Response (JSON - note: not SSE on Lambda):
{
  "content": "Ah, a roux! This is a fundamental...",
  "model": "CLAUDE_3_HAIKU",
  "modelDisplayName": "Claude 3 Haiku",
  "chunksReceived": 156
}
data:, a
data: roux
data:! This
data: is
data: a
...
```

### Model Comparison

```bash
curl "http://localhost:8080/api/v1/inference/compare?\
message=What%20is%20beurre%20blanc?&\
temperature=0"

# Response:
=== Model Comparison Results ===

[Claude 3.5 Sonnet]
Response: Ah, beurre blanc! ...
Metrics: TTFT=245ms, total=7744ms

---

[Claude 3 Haiku]
Response: Beurre blanc is ...
Metrics: TTFT=89ms, total=4495ms
```

### Custom System Prompt (POST)

```bash
curl -X POST "http://localhost:8080/api/v1/inference/stream/CLAUDE_3_5_SONNET" \
  -H "Content-Type: application/json" \
  -d '{
    "message": "Explain recursion",
    "systemPrompt": "You are a patient computer science teacher.",
    "temperature": 0.7,
    "maxTokens": 1000
  }'
```

---

## Testing

### Unit Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=ModelComparisonTest
```

### Testing the Deployed Lambda API

After running `terraform apply`, use the API Gateway URL from the outputs:

```bash
# Set your API URL (from terraform output)
export API_URL="https://YOUR_API_ID.execute-api.us-east-1.amazonaws.com"

# 1. Health check
curl "$API_URL/api/v1/inference/health"
# Expected: {"status":"OK","service":"bedrock-inference-lab"}

# 2. List available models
curl "$API_URL/api/v1/inference/models"
# Expected: [{"enumName":"CLAUDE_3_5_SONNET",...},{"enumName":"CLAUDE_3_HAIKU",...}]

# 3. Inference with Claude 3 Haiku (fast, cheap)
curl "$API_URL/api/v1/inference/stream/CLAUDE_3_HAIKU?message=Hello%20how%20are%20you"
# Expected: JSON with "content" field containing the response

# 4. Inference with Claude 3.5 Sonnet (higher quality)
curl "$API_URL/api/v1/inference/stream/CLAUDE_3_5_SONNET?message=Tell%20me%20a%20joke"

# 5. Non-streaming inference with metrics
curl "$API_URL/api/v1/inference/CLAUDE_3_HAIKU?message=What%20is%202%2B2"
# Returns response with performance metrics

# 6. POST with custom parameters
curl -X POST "$API_URL/api/v1/inference/stream/CLAUDE_3_HAIKU" \
  -H "Content-Type: application/json" \
  -d '{"message":"What is your favorite dish?","temperature":0.9}'
```

### Local Development Testing

For local development (without Lambda):

```bash
# Start the application locally
mvn spring-boot:run

# Test local endpoints (uses WebFlux streaming)
curl -N "http://localhost:8080/api/v1/inference/stream/CLAUDE_3_HAIKU?message=Hello"
```

### Manual Testing with curl (Legacy - Local Mode)

```bash
# 1. Health check
curl http://localhost:8080/api/v1/inference/health
# Expected: OK

# 2. List models
curl http://localhost:8080/api/v1/inference/models | jq
# Expected: JSON array of available models

# 3. Streaming test (Claude 3.5 Sonnet)
curl -N "http://localhost:8080/api/v1/inference/stream/CLAUDE_3_5_SONNET?\
message=How%20do%20I%20make%20a%20perfect%20omelette?&\
temperature=0"

# 4. Streaming test (Claude 3 Haiku - faster)
curl -N "http://localhost:8080/api/v1/inference/stream/CLAUDE_3_HAIKU?\
message=What%20is%20a%20bechamel?&\
temperature=0"

# 5. Model comparison
curl "http://localhost:8080/api/v1/inference/compare?\
message=Explain%20the%20five%20French%20mother%20sauces&\
temperature=0"
```

---

## Infrastructure as Code

> 💡 **What is Infrastructure as Code (IaC)?** Instead of clicking buttons in the AWS Console, you write code (Terraform files) that describes your infrastructure. This code can be version-controlled, reviewed, and applied consistently.

**Why IaC matters:**
- **Reproducibility** - Deploy the same infrastructure in dev, staging, and prod
- **Documentation** - Your infrastructure is self-documenting
- **Version Control** - Track changes, roll back if needed
- **Collaboration** - Review infrastructure changes like code

**Terraform Basics:**
```hcl
# This is HCL (HashiCorp Configuration Language)

# Define a resource (what you want to create)
resource "aws_s3_bucket" "logs" {
  bucket = "my-app-logs"
}

# Reference another resource
resource "aws_s3_bucket_versioning" "logs" {
  bucket = aws_s3_bucket.logs.id  # References the bucket above
  versioning_configuration {
    status = "Enabled"
  }
}
```

📚 [Terraform Tutorials](https://developer.hashicorp.com/terraform/tutorials)

### Terraform Resources Created

| Resource | Purpose |
|----------|---------|
| `aws_kms_key` | Encrypts S3 and CloudWatch data |
| `aws_s3_bucket` | Stores invocation logs long-term |
| `aws_s3_bucket_versioning` | Enables object versioning |
| `aws_s3_bucket_lifecycle_configuration` | Auto-transitions to Glacier, expires after 90 days |
| `aws_s3_bucket_server_side_encryption_configuration` | KMS encryption |
| `aws_s3_bucket_public_access_block` | Blocks all public access |
| `aws_cloudwatch_log_group` | Real-time log access |
| `aws_iam_role` | Allows Bedrock to write logs |
| `aws_bedrock_model_invocation_logging_configuration` | Enables logging |

### Terraform Commands

```bash
cd terraform

# Initialize (download providers)
terraform init

# Preview changes
terraform plan

# Apply changes
terraform apply

# Destroy (cleanup)
terraform destroy

# Show current state
terraform show
```

### Cost Considerations

| Resource | Cost |
|----------|------|
| S3 Storage | ~$0.023/GB/month (Standard), less for Glacier |
| CloudWatch Logs | ~$0.50/GB ingested |
| KMS | $1/month per key + $0.03/10,000 requests |
| Bedrock (Claude 3.5 Sonnet) | $3/M input tokens, $15/M output tokens |
| Bedrock (Claude 3 Haiku) | $0.25/M input tokens, $1.25/M output tokens |

---

## Learning Outcomes

After studying this POC, you will understand:

### AWS & Cloud Architecture
- How Amazon Bedrock works as a managed LLM service
- The Converse API and its advantages
- Model Invocation Logging for compliance and debugging
- Infrastructure as Code with Terraform modules
- IAM roles and policies for service-to-service communication
- KMS encryption for data at rest
- **Lambda deployment with API Gateway HTTP API**
- **SnapStart for fast Java cold starts**

### Java & Spring Development
- Spring Boot on Lambda with `aws-serverless-java-container`
- Project Reactor (Flux, Sinks) for streaming
- AWS SDK v2 async clients
- Proper exception handling with @RestControllerAdvice
- Configuration with @Value and application.yml
- **Why WebFlux doesn't work on Lambda (servlet adapter limitation)**
- **Converting reactive code to blocking for Lambda compatibility**

### GenAI Best Practices
- System prompts for consistent AI behavior
- Inference parameters (temperature, topP, etc.)
- Streaming vs buffered responses (Lambda limitations)
- Model comparison methodologies
- Enterprise-grade error handling (retries, backoff)

### Software Engineering
- Clean architecture (Controller → Service → Client)
- Immutable data with Java records
- Enum patterns for type safety
- Comprehensive testing strategies

### Lambda Deployment Lessons Learned

This POC encountered and solved several challenges deploying Spring Boot to Lambda:

| Challenge | Root Cause | Solution |
|-----------|------------|----------|
| Empty response bodies | WebFlux + servlet adapter incompatibility | Removed `spring-boot-starter-webflux`, use blocking calls |
| Jackson version conflict | X-Ray SDK uses deprecated `CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES` | Excluded `aws-xray-recorder-sdk-aws-sdk-v2-instrumentor` |
| Reactive types not serialized | `Mono`/`Flux` don't work through servlet proxy | Use `.block()` to convert to synchronous responses |
| API Gateway invokes old code | Lambda alias pointed to old version | Publish new version + update alias after code changes |

> **Key Insight:** When using `aws-serverless-java-container-springboot3`, you MUST use servlet mode (`web-application-type: servlet`) and avoid WebFlux. The adapter proxies requests through an embedded Tomcat, not Netty.

---

## Exercises for Junior Developers

> 💡 **Practice makes perfect!** Try these exercises to deepen your understanding.

### Exercise 1: Change the Persona (Easy)
**Goal:** Modify the system prompt to create a different AI persona.

1. Open `InferenceService.java`
2. Find the `JACQUES_MONTAGNE_SYSTEM_PROMPT` constant
3. Change it to a different persona (e.g., "You are a friendly Python tutor...")
4. Test with `curl -N "http://localhost:8080/api/v1/inference/stream/CLAUDE_3_HAIKU?message=Hello"`

### Exercise 2: Add a New Model (Medium)
**Goal:** Add support for another Bedrock model.

1. Open `ModelType.java`
2. Add a new enum value for Llama or Titan
3. Update any switch statements that handle model types
4. Test the new model

### Exercise 3: Add Token Counting (Medium)
**Goal:** Track input/output tokens for cost estimation.

1. Research the `ConverseStreamMetadataEvent` in the Bedrock API
2. Extract `inputTokens` and `outputTokens` from the response
3. Log them or return them in the API response

### Exercise 4: Deploy to Lambda (Advanced)
**Goal:** Make this application run on AWS Lambda.

1. Add the `aws-serverless-java-container-springboot3` dependency
2. Create a Lambda handler class
3. Deploy using AWS SAM or Terraform
4. Test the serverless endpoint

### Exercise 5: Add Caching (Advanced)
**Goal:** Cache repeated prompts to reduce costs.

1. Implement a simple in-memory cache (or Redis)
2. Hash the prompt + model + parameters as a key
3. Return cached responses for identical requests
4. Consider cache invalidation strategies

---

## Troubleshooting

### Common Issues

| Issue | Cause | Solution |
|-------|-------|----------|
| `AccessDeniedException` | Model not enabled | Enable model in Bedrock console |
| `ThrottlingException` | Too many requests | Implement backoff, increase quotas |
| `Connection timeout` | Network issues | Check VPC, security groups |
| `ModelNotReadyException` | Cold start | Retry after delay |
| Empty streaming response | Model access | Verify model is enabled for your account |

### Lambda-Specific Issues

| Issue | Cause | Solution |
|-------|-------|----------|
| HTTP 200 but empty body | WebFlux on classpath with servlet adapter | Remove `spring-boot-starter-webflux`, use blocking calls |
| `NoSuchFieldError: CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES` | Jackson version conflict with X-Ray SDK | Exclude `aws-xray-recorder-sdk-aws-sdk-v2-instrumentor` from powertools-tracing |
| API returns old code after deploy | Lambda alias points to old version | Publish new version: `aws lambda publish-version`, then update alias |
| Slow cold starts (5-10s) | Java/Spring initialization | Enable SnapStart in Terraform, use lazy initialization |
| Lambda timeout | Response takes too long | Increase timeout in Terraform (default 120s), check Bedrock latency |

### Debugging Lambda

```bash
# Check Lambda logs
aws logs tail /aws/lambda/bedrock-inference-lab-dev --since 5m --region us-east-1

# Invoke Lambda directly (bypassing API Gateway)
aws lambda invoke --function-name bedrock-inference-lab-dev:live \
  --region us-east-1 \
  --payload '{"version":"2.0","rawPath":"/api/v1/inference/health","requestContext":{"http":{"method":"GET","path":"/api/v1/inference/health"}}}' \
  --cli-binary-format raw-in-base64-out \
  response.json && cat response.json

# Check Lambda configuration
aws lambda get-function --function-name bedrock-inference-lab-dev --region us-east-1
```

### Checking Model Access

```bash
# List all available models
aws bedrock list-foundation-models --query "modelSummaries[].modelId" --region us-east-1

# Check specific model
aws bedrock get-foundation-model --model-identifier anthropic.claude-3-5-sonnet-20240620-v1:0 --region us-east-1
```

---

## Next Steps

This POC is part of a larger learning path:

| POC | Focus | Status |
|-----|-------|--------|
| **POC-01** | **Bedrock Inference & Observability** | **Complete** |
| POC-02 | RAG Pipeline & Knowledge Bases | Planned |
| POC-03 | Agentic Tools (SQL queries) | Planned |
| POC-04 | Security Guardrails (PII/GDPR) | Planned |
| POC-05 | Automated Evaluation (Ragas) | Planned |
| POC-06 | Multi-modal Vision | Planned |
| POC-07 | Model Fine-tuning | Planned |

---

## Author

**Jacques Montagne** - Demonstrating AWS GenAI architecture for enterprise applications.

---

## License

This project is for educational purposes.

---

## Further Reading

### Official Documentation

| Resource | Link | Description |
|----------|------|-------------|
| Amazon Bedrock | [User Guide](https://docs.aws.amazon.com/bedrock/latest/userguide/what-is-bedrock.html) | Complete Bedrock documentation |
| AWS SDK for Java v2 | [Developer Guide](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/home.html) | Java SDK documentation |
| Spring WebFlux | [Reference](https://docs.spring.io/spring-framework/reference/web/webflux.html) | Reactive web framework docs |
| Project Reactor | [Reference Guide](https://projectreactor.io/docs/core/release/reference/) | Reactive programming library |
| Terraform AWS | [Provider Docs](https://registry.terraform.io/providers/hashicorp/aws/latest/docs) | Terraform AWS resource reference |

### Tutorials & Courses

| Resource | Link | Description |
|----------|------|-------------|
| AWS Skill Builder | [GenAI Learning Path](https://explore.skillbuilder.aws/learn/course/external/view/elearning/17763/amazon-bedrock-getting-started) | Free AWS GenAI courses |
| LangChain4j | [Tutorials](https://docs.langchain4j.dev/tutorials/) | Step-by-step LangChain4j guides |
| Baeldung Reactor | [Guide](https://www.baeldung.com/reactor-core) | Practical Project Reactor tutorial |

### Books (Recommended)

| Book | Author | Why Read It |
|------|--------|-------------|
| "Reactive Programming with RxJava" | Tomasz Nurkiewicz | Deep dive into reactive patterns |
| "Spring in Action" | Craig Walls | Comprehensive Spring Framework guide |
| "Terraform: Up & Running" | Yevgeniy Brikman | Best practices for IaC |

### Community Resources

- [AWS re:Post](https://repost.aws/) - AWS community Q&A
- [Stack Overflow - amazon-bedrock](https://stackoverflow.com/questions/tagged/amazon-bedrock) - Bedrock questions
- [r/aws](https://www.reddit.com/r/aws/) - AWS subreddit
- [LangChain4j Discord](https://discord.gg/langchain4j) - Community chat


