# POC-02: RAG Pipeline with Amazon Bedrock Knowledge Bases

## What You Will Learn

This POC teaches you how to build a complete **Retrieval-Augmented Generation (RAG)** pipeline using Amazon Bedrock Knowledge Bases. By the end, you will understand:

1. **What RAG is** and why it solves the hallucination problem
2. **How documents become searchable** through chunking and embedding
3. **Two retrieval patterns** — direct chunk retrieval vs. end-to-end generation
4. **How to evaluate RAG quality** using the LLM-as-judge pattern
5. **Infrastructure as Code** for the entire pipeline using Terraform

> **AIP-C01 Exam Relevance:** RAG accounts for roughly 15-20% of the exam. You need to understand when to choose Knowledge Bases over custom vector stores, the trade-offs between chunking strategies, and how to measure retrieval quality.

---

## Core Concepts Explained

### What is RAG?

Large Language Models (LLMs) are trained on a fixed dataset. When you ask about your company's internal documents, the model has no knowledge of them and may "hallucinate" — confidently generate incorrect answers.

**RAG solves this** by adding a retrieval step before generation:

```
User Query
    │
    ▼
┌─────────────────────┐
│  1. RETRIEVE         │  Search a vector database for relevant document chunks
│     (Embedding +     │  that match the user's query
│      Similarity)     │
└──────────┬──────────┘
           │  relevant chunks
           ▼
┌─────────────────────┐
│  2. AUGMENT          │  Inject those chunks into the LLM prompt as context:
│     (Prompt          │  "Based on the following documents: {chunks},
│      Construction)   │   answer: {query}"
└──────────┬──────────┘
           │  enriched prompt
           ▼
┌─────────────────────┐
│  3. GENERATE         │  The LLM answers using ONLY the provided context,
│     (LLM Call)       │  reducing hallucinations and enabling source citations
└─────────────────────┘
```

The key insight: **the model does not need to "know" the answer — it just needs to read and summarize the relevant documents you provide.**

### How Documents Become Searchable

Before you can retrieve anything, documents must go through an ingestion pipeline:

```
Source Documents (S3)
    │
    ▼
┌─────────────────────┐
│  CHUNKING            │  Split large documents into smaller pieces.
│                      │  Why? Embedding models have token limits,
│                      │  and smaller chunks are more precise to retrieve.
└──────────┬──────────┘
           │  chunks (300-500 tokens each)
           ▼
┌─────────────────────┐
│  EMBEDDING           │  Convert each text chunk into a numeric vector
│                      │  (a list of 1024 floating-point numbers).
│  Model: Titan V2     │  Semantically similar texts produce similar vectors.
└──────────┬──────────┘
           │  vectors
           ▼
┌─────────────────────┐
│  VECTOR STORE        │  Store vectors in a database optimized for
│  (OpenSearch)        │  similarity search (nearest-neighbor lookup).
└─────────────────────┘
```

This is exactly what Amazon Bedrock Knowledge Bases manages for you automatically.

### Two Retrieval Patterns

This POC implements both patterns so you can compare them:

| Pattern | API | What You Get | When to Use |
|---------|-----|--------------|-------------|
| **Retrieve** | `RetrieveRequest` | Raw document chunks with scores | When you want control over the generation step (custom prompts, post-processing) |
| **RetrieveAndGenerate** | `RetrieveAndGenerateRequest` | A generated answer with citations | When you want an end-to-end answer with source attribution out of the box |

> **Exam Tip:** The exam tests whether you know the difference. `Retrieve` gives you chunks; `RetrieveAndGenerate` gives you a complete answer. Use `Retrieve` when you need to customize the prompt template or chain multiple steps.

---

## Architecture

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                          POC-02: RAG Pipeline                                │
│                                                                              │
│  ┌─────────────┐         ┌───────────────────────────────────────────────┐  │
│  │             │         │         Amazon Bedrock Knowledge Base          │  │
│  │     S3      │         │  ┌─────────────────────────────────────────┐  │  │
│  │   Bucket    │────────▶│  │          Data Source (S3)                │  │  │
│  │             │  sync   │  │  - Chunking: Fixed Size (300 tokens)    │  │  │
│  │  /docs/     │         │  │  - Overlap: 20%                         │  │  │
│  │             │         │  └─────────────────────────────────────────┘  │  │
│  └─────────────┘         │                    │                          │  │
│                          │                    ▼                          │  │
│                          │  ┌─────────────────────────────────────────┐  │  │
│                          │  │        Titan Embeddings V2              │  │  │
│                          │  │        1024 dimensions, 8K tokens       │  │  │
│                          │  └─────────────────────────────────────────┘  │  │
│                          │                    │                          │  │
│                          │                    ▼                          │  │
│                          │  ┌─────────────────────────────────────────┐  │  │
│                          │  │        Managed Vector Store             │  │  │
│                          │  │        (OpenSearch Serverless)          │  │  │
│                          │  └─────────────────────────────────────────┘  │  │
│                          └──────────────────┬────────────────────────────┘  │
│                                             │                               │
│                                   Bedrock Agent Runtime API                 │
│                                             │                               │
│  ┌──────────────────────────────────────────▼────────────────────────────┐  │
│  │              Java Application (Spring Boot 3.4)                       │  │
│  │                                                                       │  │
│  │  ┌──────────────────┐  ┌──────────────────┐  ┌───────────────────┐   │  │
│  │  │RetrievalService  │  │   RagService     │  │EvaluationService  │   │  │
│  │  │                  │  │                  │  │                   │   │  │
│  │  │ retrieve()       │  │ generate()       │  │ relevance()       │   │  │
│  │  │ + filter support │  │ + citations      │  │ groundedness()    │   │  │
│  │  └──────────────────┘  └──────────────────┘  └───────────────────┘   │  │
│  │                                                                       │  │
│  │  ┌──────────────────┐  ┌──────────────────────────────────────────┐  │  │
│  │  │  SyncService     │  │            REST API (/api/v1)            │  │  │
│  │  │                  │  │                                          │  │  │
│  │  │ startSync()      │  │  POST /retrieve    POST /generate       │  │  │
│  │  │ getSyncStatus()  │  │  POST /evaluate    POST /sync/{id}      │  │  │
│  │  └──────────────────┘  │  GET  /sync/{id}/status/{jobId}         │  │  │
│  │                        │  GET  /health                            │  │  │
│  │                        └──────────────────────────────────────────┘  │  │
│  └──────────────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────────────────┘
```

---

## Tech Stack

| Layer | Technology | Version | Why This Choice |
|-------|------------|---------|-----------------|
| Infrastructure | Terraform | >= 1.5 | Reproducible, auditable AWS resource management |
| Storage | Amazon S3 | - | Scalable document storage with versioning |
| Knowledge Base | Bedrock KB | - | Fully managed RAG — no vector DB ops needed |
| Embeddings | Titan Embeddings V2 | v2:0 | AWS-native, 1024 dimensions, 8K token context |
| Vector Store | OpenSearch Serverless | - | Managed by KB — zero operational overhead |
| LLM | Claude 3 Sonnet | v1 | Strong reasoning with source attribution |
| Application | Java 21 | 21 | Modern Java with virtual threads support |
| Framework | Spring Boot | 3.4 | Production-ready REST API framework |
| AWS SDK | AWS SDK v2 | 2.29+ | Async clients via `CompletableFuture` |

---

## Project Structure

```
POC-02-rag-pipeline/
├── terraform/                          # Infrastructure as Code
│   ├── main.tf                         # AWS provider configuration
│   ├── variables.tf                    # Input variables (region, project name)
│   ├── s3.tf                           # Document storage bucket
│   ├── iam.tf                          # Knowledge Base execution role
│   ├── knowledge-base.tf               # Bedrock KB + S3 data source
│   ├── opensearch.tf                   # OpenSearch Serverless collection
│   ├── outputs.tf                      # Exported values (KB ID, bucket name)
│   └── terraform.tfvars.example        # Example variable values
│
├── src/main/java/com/awslab/rag/
│   ├── RagApplication.java             # Spring Boot entry point
│   ├── config/
│   │   ├── BedrockConfig.java          # Three async AWS client beans
│   │   └── RagProperties.java          # KB configuration properties
│   ├── controller/
│   │   └── RagController.java          # REST API endpoints
│   ├── service/
│   │   ├── RetrievalService.java       # Direct chunk retrieval from KB
│   │   ├── RagService.java             # End-to-end RAG with citations
│   │   ├── EvaluationService.java      # LLM-as-judge quality metrics
│   │   └── SyncService.java            # KB document ingestion management
│   ├── model/                          # Request/Response DTOs
│   │   ├── RetrievalRequest.java       #   with Jakarta validation
│   │   ├── RetrievalResponse.java
│   │   ├── GenerateRequest.java
│   │   ├── GenerateResponse.java
│   │   ├── EvaluationRequest.java
│   │   ├── EvaluationResponse.java
│   │   └── Citation.java
│   └── exception/
│       ├── RagException.java           # Domain exception with error codes
│       └── GlobalExceptionHandler.java # Centralized HTTP error mapping
│
├── src/test/java/com/awslab/rag/      # Unit tests (62 tests, pure mocks)
│   ├── service/
│   │   ├── RetrievalServiceTest.java   # 11 tests
│   │   ├── RagServiceTest.java         # 10 tests
│   │   ├── EvaluationServiceTest.java  # 9 tests
│   │   └── SyncServiceTest.java        # 10 tests
│   ├── exception/
│   │   └── GlobalExceptionHandlerTest.java  # 9 tests
│   └── controller/
│       └── RagControllerTest.java      # 12 tests (@WebMvcTest)
├── src/test/resources/
│   └── application-test.yml            # Test configuration (no AWS needed)
│
├── test-docs/                          # Sample documents for Knowledge Base
│   ├── aws-well-architected.md         # + .metadata.json for each
│   ├── bedrock-pricing.md
│   └── lambda-best-practices.md
│
├── scripts/
│   ├── sync-knowledge-base.sh          # Trigger KB document ingestion
│   └── test-queries.sh                 # Sample API calls for manual testing
│
├── pom.xml                             # Maven build configuration
└── README.md
```

---

## Deep Dive: Chunking Strategies

Chunking is one of the most impactful decisions in a RAG pipeline. It determines how your documents are split before embedding. A poor chunking strategy leads to irrelevant retrievals, regardless of how good your LLM is.

### Why Chunk at All?

1. **Embedding models have token limits** — Titan V2 accepts up to 8K tokens, but shorter texts produce more focused embeddings
2. **Precision** — a 300-token chunk about "Lambda cold starts" is more retrievable than an entire 50-page PDF
3. **Cost** — each retrieved chunk becomes part of the LLM prompt; fewer, more relevant chunks = lower token costs

### Strategy Comparison

| Strategy | How It Works | Best For | Trade-off |
|----------|-------------|----------|-----------|
| **Fixed Size** | Split every N tokens with M% overlap | Code, uniform docs | Simple but may cut mid-sentence |
| **Hierarchical** | Parent chunks (1500 tokens) contain child chunks (300 tokens); retrieval uses children, context uses parents | Structured docs (PDF, HTML with sections) | Better context preservation, more storage |
| **Semantic** | Split at natural topic boundaries detected by the embedding model | Narratives, articles | Best coherence, but slower and variable chunk sizes |
| **None** | Keep entire document as one chunk | Small docs (< 300 tokens) | Full context, but only for tiny files |

### This POC Uses: Fixed Size

```hcl
# In terraform/knowledge-base.tf
chunking_configuration {
  chunking_strategy = "FIXED_SIZE"
  fixed_size_chunking_configuration {
    max_tokens         = 300    # Each chunk is at most 300 tokens
    overlap_percentage = 20     # 20% overlap between consecutive chunks
  }
}
```

**Why overlap?** Without it, a sentence split across two chunks loses its meaning in both. A 20% overlap ensures boundary sentences appear in at least one complete chunk.

> **Exam Tip:** When asked about chunking for technical documentation with code samples, choose **Fixed Size** with higher overlap (20%). For legal documents with clear sections, choose **Hierarchical**. For narrative content like articles, choose **Semantic**.

---

## Deep Dive: Search Types

When retrieving chunks, you choose how the vector database matches your query:

| Search Type | How It Works | Strengths | Weaknesses |
|-------------|-------------|-----------|------------|
| **SEMANTIC** | Converts query to a vector and finds nearest neighbors | Understands meaning ("car" matches "vehicle") | May miss exact keyword matches |
| **HYBRID** | Combines semantic similarity with keyword (BM25) search | Best of both worlds — meaning + exact terms | Slightly higher latency |

This POC uses **HYBRID** as the default because it consistently produces the best results for mixed-content document sets.

---

## Deep Dive: Evaluation with LLM-as-Judge

How do you know if your RAG pipeline is working well? You cannot manually review every response. Instead, this POC uses a pattern called **LLM-as-judge**: ask an LLM to rate the quality of another LLM's output.

### Two Metrics Implemented

**1. Relevance** — Are the retrieved chunks actually relevant to the query?

```
Prompt to judge LLM:
  "Rate the relevance of these chunks to the query on a scale of 0-10.
   Query: {user_query}
   Chunks: {retrieved_chunks}
   Return ONLY a number."

Score = LLM_response / 10.0    →    0.0 to 1.0
```

**2. Groundedness** — Is the generated answer supported by the retrieved chunks?

```
Prompt to judge LLM:
  "Rate how well this answer is supported by the source chunks.
   Answer: {generated_answer}
   Chunks: {retrieved_chunks}
   Return ONLY a number."

Score = LLM_response / 10.0    →    0.0 to 1.0
```

**Why this matters:** A relevance score of 0.3 tells you your retrieval needs tuning (wrong chunking, too few results). A groundedness score of 0.4 tells you the LLM is fabricating instead of using the provided context.

> **Exam Tip:** The exam distinguishes between these metrics. Relevance measures retrieval quality. Groundedness (also called "faithfulness") measures generation quality. You need both to evaluate a full RAG pipeline.

---

## How to Run

### Prerequisites

- Java 21
- Maven 3.9+
- AWS CLI configured with appropriate credentials
- Terraform >= 1.5

### Step 1: Deploy Infrastructure

```bash
cd POC-02-rag-pipeline/terraform/
cp terraform.tfvars.example terraform.tfvars   # Edit with your values
terraform init
terraform plan
terraform apply
```

Note the outputs: `knowledge_base_id`, `data_source_id`, `bucket_name`.

### Step 2: Upload Documents and Sync

```bash
# Upload test documents to S3
aws s3 sync test-docs/ s3://<bucket_name>/

# Trigger Knowledge Base ingestion
./scripts/sync-knowledge-base.sh <knowledge_base_id> <data_source_id>
```

The sync job reads documents from S3, chunks them, generates embeddings, and stores vectors in OpenSearch. This runs asynchronously — use the sync status endpoint to monitor progress.

### Step 3: Run the Application

```bash
export KNOWLEDGE_BASE_ID=<your-kb-id>
mvn spring-boot:run
```

The API will be available at `http://localhost:8080`.

### Step 4: Run Tests (No AWS Credentials Needed)

```bash
mvn test
```

All 62 unit tests use mocked AWS SDK clients and run without any AWS infrastructure.

---

## API Reference

### POST /api/v1/retrieve — Direct Chunk Retrieval

Retrieves raw document chunks from the Knowledge Base without generating an answer. Use this when you want full control over what happens with the retrieved context.

```bash
curl -X POST http://localhost:8080/api/v1/retrieve \
  -H "Content-Type: application/json" \
  -d '{
    "query": "What are the pillars of AWS Well-Architected Framework?",
    "numberOfResults": 5,
    "searchType": "HYBRID",
    "filter": {"category": "architecture"}
  }'
```

**Response:** List of chunks with content, source URI, relevance score, and metadata.

### POST /api/v1/generate — End-to-End RAG

Retrieves relevant chunks AND generates a complete answer with source citations.

```bash
curl -X POST http://localhost:8080/api/v1/generate \
  -H "Content-Type: application/json" \
  -d '{
    "query": "How should I optimize Lambda cold starts?",
    "numberOfResults": 5,
    "temperature": 0.0,
    "maxTokens": 1024
  }'
```

**Response:** Generated answer text, list of citations (with source URIs and generated span positions), and latency.

### POST /api/v1/evaluate — RAG Quality Evaluation

Evaluates the quality of retrieval and generation using the LLM-as-judge pattern.

```bash
curl -X POST http://localhost:8080/api/v1/evaluate \
  -H "Content-Type: application/json" \
  -d '{
    "query": "What are Lambda best practices?",
    "answer": "Use provisioned concurrency to reduce cold starts...",
    "retrievedChunks": ["Lambda cold starts occur when...", "Best practices include..."]
  }'
```

**Response:** Relevance score (0.0-1.0), groundedness score (0.0-1.0), and latency.

### POST /api/v1/sync/{dataSourceId} — Start Document Sync

Triggers an ingestion job to sync new or updated documents from S3 into the Knowledge Base.

```bash
curl -X POST http://localhost:8080/api/v1/sync/ds-001
```

**Response:** Job ID, status, and start timestamp.

### GET /api/v1/sync/{dataSourceId}/status/{jobId} — Check Sync Status

```bash
curl http://localhost:8080/api/v1/sync/ds-001/status/job-001
```

**Response:** Job status (STARTING, IN_PROGRESS, COMPLETE, FAILED), timestamps, document statistics, and failure reasons if applicable.

### GET /api/v1/health — Health Check

```bash
curl http://localhost:8080/api/v1/health
```

**Response:** `{"status": "UP", "service": "rag-pipeline"}`

---

## Metadata Filtering

Not all documents are relevant to every query. Metadata filtering lets you narrow retrieval to specific document categories before similarity search runs.

### How It Works

Each document in S3 can have a companion `.metadata.json` file:

```json
{
  "metadataAttributes": {
    "category": "security",
    "year": 2024,
    "author": "aws"
  }
}
```

When you pass a `filter` in the retrieve request, only chunks from matching documents are considered:

```json
{
  "query": "What are the security best practices?",
  "filter": {"category": "security"}
}
```

### Single vs. Multiple Filters

- **One filter field** → uses an `equals` condition
- **Multiple filter fields** → combines them with `AND` (all conditions must match)

This is implemented in `RetrievalService.buildFilter()` using the AWS SDK's `RetrievalFilter` with `equalsValue()` and `andAll()`.

---

## Experiments to Try

These experiments help you build intuition for exam questions about RAG tuning.

### Experiment 1: Chunking Strategy Comparison

1. Create 3 Knowledge Bases with Fixed, Hierarchical, and Semantic chunking
2. Upload the same `test-docs/` to each
3. Run the same 10 queries against each using `/api/v1/retrieve`
4. Compare relevance scores using `/api/v1/evaluate`

| Strategy | Expected Avg Relevance | Expected Avg Latency | Best For |
|----------|----------------------|---------------------|----------|
| Fixed (300) | ~0.75 | ~120ms | General purpose |
| Hierarchical | ~0.82 | ~150ms | Structured documents |
| Semantic | ~0.85 | ~200ms | Narratives and articles |

### Experiment 2: Number of Results

Vary `numberOfResults` (3, 5, 10, 20) and observe:
- **More results** = more context for the LLM = potentially better answers, but also more noise and higher token cost
- **Fewer results** = more focused, cheaper, but might miss relevant information

### Experiment 3: SEMANTIC vs. HYBRID Search

Run identical queries with each search type and compare:
- **SEMANTIC** is better when queries use different words than the documents
- **HYBRID** is better when exact terms matter (product names, error codes)

---

## Cost Analysis

| Resource | Pricing | POC Estimate |
|----------|---------|--------------|
| Bedrock KB (managed store) | Included | $0 |
| OpenSearch Serverless (managed) | Included with KB | $0 |
| Titan Embeddings V2 | $0.00002/1K tokens | ~$0.10/month |
| Claude 3 Sonnet | $0.003/1K input, $0.015/1K output | ~$2/month |
| S3 Storage | $0.023/GB | ~$0.01/month |
| **Total POC Cost** | | **< $3/month** |

> **Key Learning:** Using Bedrock's managed Knowledge Base avoids the $700+/month minimum cost of self-managed OpenSearch Serverless. This is a critical cost optimization point for the exam.

---

## Troubleshooting

| Symptom | Likely Cause | Solution |
|---------|-------------|----------|
| KB sync fails | Missing IAM permissions | Verify role has `s3:GetObject` and `s3:ListBucket` |
| Empty retrieval results | Sync not complete | Check sync status — ingestion is async |
| Low relevance scores | Suboptimal chunking | Try Hierarchical for structured docs, increase overlap |
| High retrieval latency | Knowledge Base cold start | KB scales down after inactivity; first request is slower |
| "Model not available" error | Region mismatch | Model ARN region must match KB region |
| Throttling errors (429) | Too many concurrent requests | Implement exponential backoff (handled by `RagException.THROTTLING`) |

---

## Exam Topics Covered (AIP-C01)

| Domain | Topic | Where in This POC |
|--------|-------|--------------------|
| **Domain 1** | Knowledge Base architecture | `terraform/knowledge-base.tf`, `BedrockConfig.java` |
| **Domain 1** | Chunking strategies | Terraform chunking config, Deep Dive section above |
| **Domain 1** | Embedding model selection | Titan V2 in `knowledge-base.tf` and `application.yml` |
| **Domain 2** | Retrieve API | `RetrievalService.java` — direct chunk retrieval |
| **Domain 2** | RetrieveAndGenerate API | `RagService.java` — end-to-end RAG with citations |
| **Domain 2** | Metadata filtering | `RetrievalService.buildFilter()` with `equalsValue()` / `andAll()` |
| **Domain 2** | Source attribution | `RagService.extractCitations()` — span mapping |
| **Domain 3** | RAG evaluation metrics | `EvaluationService.java` — LLM-as-judge pattern |
| **Domain 4** | Cost optimization | Managed KB vs. self-hosted comparison |

---

## Success Criteria

- [ ] Terraform deploys all resources without errors
- [ ] Knowledge Base syncs documents from S3 successfully
- [ ] Retrieve API returns relevant chunks (relevance > 0.7)
- [ ] RetrieveAndGenerate produces answers grounded in source documents
- [ ] Citations correctly reference source S3 URIs
- [ ] Metadata filtering narrows results to matching documents only
- [ ] Evaluation service computes relevance and groundedness scores
- [ ] All 62 unit tests pass (`mvn test`)
- [ ] You can explain chunking trade-offs for the exam

---

## References

- [Amazon Bedrock Knowledge Bases — User Guide](https://docs.aws.amazon.com/bedrock/latest/userguide/knowledge-base.html)
- [Chunking and Parsing Configuration](https://docs.aws.amazon.com/bedrock/latest/userguide/kb-chunking-parsing.html)
- [RetrieveAndGenerate API Reference](https://docs.aws.amazon.com/bedrock/latest/APIReference/API_agent-runtime_RetrieveAndGenerate.html)
- [Terraform: aws_bedrockagent_knowledge_base](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/bedrockagent_knowledge_base)
- [AWS SDK for Java v2 — Bedrock Agent Runtime](https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/bedrockagentruntime/package-summary.html)
