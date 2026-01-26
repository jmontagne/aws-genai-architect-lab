# POC-01: Bedrock Inference & Observability Lab

## 🎯 Objective
Master the technical implementation of Amazon Bedrock foundational models with a focus on **deterministic control**, **streaming performance**, and **enterprise-grade observability**. This POC serves as the "Inference Baseline" for the Safe Modernization strategy.

## 🏗️ Architecture & Stack
* **Infrastructure (IaC):** Terraform (IAM roles, S3 Logging, Bedrock Settings).
* **Application:** Java 21 / Spring Boot 3.4 / LangChain4j.
* **Runtime:** AWS SDK for Java v2 (Asynchronous Client).
* **Observability:** Amazon CloudWatch Logs (Model Invocation Logging).



## 📝 Technical Scope

### 1. Inference Control (The "Science" of Prompting)
Testing and documenting the impact of inference parameters across different model families:
* **Models:** `anthropic.claude-3-5-sonnet-v1:0` vs `meta.llama3-2-3b-instruct-v1:0`.
* **Parameters:** `temperature`, `topP`, `maxTokens`, and `stopSequences`.
* **Patterns:** Implementation of the "Jacques Montagne" System Prompt pattern for consistent persona enforcement.

### 2. Performance Engineering
* **Reactive Streaming:** Transition from `InvokeModel` (blocking) to `InvokeModelWithResponseStream` using Project Reactor (`Flux<String>`).
* **Latency Metrics:** Tracking Time To First Token (TTFT) and total generation time.

### 3. Enterprise Safety & Audit
* **Invocation Logging:** Enabling S3/CloudWatch logging for all requests (PII and Audit trail).
* **Error Handling:** Managing `ThrottlingException` and `ModelNotReadyException` with exponential backoff.

## 📂 Repository Structure
```text
POC-01-bedrock-basics/
├── terraform/               # IaC: IAM, S3, CloudWatch Logs for Bedrock
├── src/main/java/           # Java 21 / LangChain4j implementation
│   ├── client/              # Async Bedrock Client configuration
│   ├── service/             # Inference logic (Streaming vs Blocking)
│   └── model/               # Strongly typed request/response wrappers
├── scripts/                 # Python/Boto3 benchmarking & testing scripts
└── README.md                # This file
```

## 🚀 Key "Safe Modernization" Arguments
1.  **Decoupling:** Business logic is separated from the specific LLM provider (Bedrock API abstraction).
2.  **Auditability:** Every prompt and response is logged at the infrastructure level, meeting financial sector compliance.
3.  **Cost Control:** Implementation of token usage tracking to prevent "bill shock" during the modernization phase.

## 🤖 AI Builder Instructions (For Kilo Code/Copilot)
> "Acting as an AWS GenAI Architect, generate the implementation for POC-01:
> 1. **Terraform**: Create a module that enables 'Amazon Bedrock Model Invocation Logging' to an encrypted S3 bucket and CloudWatch Log Group.
> 2. **Java 21**: Use LangChain4j and `BedrockRuntimeAsyncClient`. Create a service that returns a `Flux<String>` for streaming responses. 
> 3. **Validation**: Provide a test case that compares the output of Claude 3.5 and Llama 3.2 using the same system prompt and temperature=0."
