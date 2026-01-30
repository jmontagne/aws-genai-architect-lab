# POC-02: RAG Pipeline with Amazon Bedrock Knowledge Bases

## Objective

Build a production-ready RAG (Retrieval-Augmented Generation) pipeline using **Amazon Bedrock Knowledge Bases** with managed vector store. This POC focuses on exam-relevant concepts: chunking strategies, embedding models, retrieval tuning, and evaluation patterns.

**Why this matters for AIP-C01:** RAG is ~15-20% of the exam. You must understand when to use Knowledge Bases vs. custom vector stores, chunking trade-offs, and how to evaluate retrieval quality.

---

## Architecture

```
┌────────────────────────────────────────────────────────────────────────────┐
│                         POC-02: RAG Pipeline                                │
│                                                                             │
│  ┌─────────────┐         ┌──────────────────────────────────────────────┐  │
│  │             │         │        Amazon Bedrock Knowledge Base          │  │
│  │     S3      │         │  ┌────────────────────────────────────────┐  │  │
│  │   Bucket    │────────▶│  │         Data Source (S3)               │  │  │
│  │             │  sync   │  │  - Chunking Strategy (configurable)    │  │  │
│  │  /docs/     │         │  │  - Metadata extraction                 │  │  │
│  │  /test/     │         │  └────────────────────────────────────────┘  │  │
│  │             │         │                    │                          │  │
│  └─────────────┘         │                    ▼                          │  │
│                          │  ┌────────────────────────────────────────┐  │  │
│                          │  │      Titan Embeddings V2               │  │  │
│                          │  │      (amazon.titan-embed-text-v2:0)    │  │  │
│                          │  │      1024 dimensions, 8K tokens        │  │  │
│                          │  └────────────────────────────────────────┘  │  │
│                          │                    │                          │  │
│                          │                    ▼                          │  │
│                          │  ┌────────────────────────────────────────┐  │  │
│                          │  │      Managed Vector Store              │  │  │
│                          │  │      (OpenSearch Serverless - included)│  │  │
│                          │  │      - No OCU costs                    │  │  │
│                          │  │      - Auto-scaling                    │  │  │
│                          │  └────────────────────────────────────────┘  │  │
│                          └──────────────────┬───────────────────────────┘  │
│                                             │                               │
│                                             │ BedrockAgentRuntime API       │
│                                             ▼                               │
│  ┌──────────────────────────────────────────────────────────────────────┐  │
│  │                    Java Application (Spring Boot 3)                   │  │
│  │                                                                       │  │
│  │  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────────┐   │  │
│  │  │ RetrievalService│  │   RagService    │  │ EvaluationService   │   │  │
│  │  │                 │  │                 │  │                     │   │  │
│  │  │ - retrieve()    │  │ - generate()    │  │ - relevanceScore()  │   │  │
│  │  │ - filter()      │  │ - stream()      │  │ - groundedness()    │   │  │
│  │  │ - rerank()      │  │ - cite()        │  │ - faithfulness()    │   │  │
│  │  └─────────────────┘  └─────────────────┘  └─────────────────────┘   │  │
│  │                                                                       │  │
│  │  ┌─────────────────────────────────────────────────────────────────┐ │  │
│  │  │                      REST API (/api/v1)                         │ │  │
│  │  │  POST /retrieve    POST /generate    POST /evaluate             │ │  │
│  │  └─────────────────────────────────────────────────────────────────┘ │  │
│  └──────────────────────────────────────────────────────────────────────┘  │
│                                                                             │
└────────────────────────────────────────────────────────────────────────────┘
```

---

## Tech Stack

| Layer | Technology | Version | Purpose |
|-------|------------|---------|---------|
| Infrastructure | Terraform | >= 1.5 | IaC for all AWS resources |
| Storage | Amazon S3 | - | Document source |
| Knowledge Base | Bedrock KB | - | Managed RAG infrastructure |
| Embeddings | Titan Embeddings V2 | v2:0 | Vector embeddings (1024 dim) |
| Vector Store | Managed OpenSearch | - | Included with KB, no extra cost |
| LLM | Claude 3 Sonnet | v1 | Generation model |
| Application | Java | 21 | Runtime |
| Framework | Spring Boot | 3.4 | Application framework |
| SDK | AWS SDK v2 | 2.29+ | BedrockAgentRuntimeAsyncClient |

---

## Exam Topics Covered (AIP-C01)

| Domain | Topic | Coverage in POC |
|--------|-------|-----------------|
| **Domain 1** | Knowledge Base architecture | Full implementation |
| **Domain 1** | Chunking strategies | Fixed, Hierarchical, Semantic comparison |
| **Domain 1** | Embedding model selection | Titan V2 vs alternatives |
| **Domain 2** | Retrieval API patterns | Retrieve vs RetrieveAndGenerate |
| **Domain 2** | Metadata filtering | Attribute-based filtering |
| **Domain 2** | Source attribution | Citation extraction |
| **Domain 3** | RAG evaluation metrics | Relevance, Groundedness |
| **Domain 4** | Cost optimization | Managed vs self-hosted comparison |

---

## Chunking Strategies Deep Dive

### Comparison Matrix

| Strategy | Best For | Chunk Size | Overlap | Pros | Cons |
|----------|----------|------------|---------|------|------|
| **Fixed Size** | Uniform docs, code | 300-500 tokens | 10-20% | Predictable, simple | May break context |
| **Hierarchical** | Structured docs (PDF, HTML) | Parent: 1500, Child: 300 | Automatic | Preserves structure | More storage |
| **Semantic** | Narratives, articles | Variable | Natural boundaries | Best coherence | Slower, unpredictable |
| **None** | Small docs (<300 tokens) | Full doc | N/A | Complete context | Limited to small files |

### Exam Tip
> "When asked about chunking for technical documentation with code samples, choose **Fixed Size** with higher overlap (20%). For legal documents with sections, choose **Hierarchical**."

### Configuration Examples

```hcl
# Fixed Size (Terraform)
chunking_configuration {
  chunking_strategy = "FIXED_SIZE"
  fixed_size_chunking_configuration {
    max_tokens         = 300
    overlap_percentage = 20
  }
}

# Hierarchical (Terraform)
chunking_configuration {
  chunking_strategy = "HIERARCHICAL"
  hierarchical_chunking_configuration {
    level_configuration {
      max_tokens = 1500  # Parent
    }
    level_configuration {
      max_tokens = 300   # Child
    }
    overlap_tokens = 60
  }
}
```

---

## Implementation Guide

### Project Structure

```
POC-02-rag-pipeline/
├── terraform/
│   ├── main.tf                 # Provider, backend config
│   ├── variables.tf            # Input variables
│   ├── s3.tf                   # Document bucket
│   ├── iam.tf                  # KB execution role
│   ├── knowledge-base.tf       # Bedrock KB + data source
│   ├── outputs.tf              # KB ID, bucket name, role ARN
│   └── terraform.tfvars.example
├── src/
│   └── main/
│       ├── java/com/awslab/rag/
│       │   ├── RagApplication.java
│       │   ├── config/
│       │   │   └── BedrockConfig.java
│       │   ├── controller/
│       │   │   └── RagController.java
│       │   ├── service/
│       │   │   ├── RetrievalService.java
│       │   │   ├── RagService.java
│       │   │   └── EvaluationService.java
│       │   ├── model/
│       │   │   ├── RetrievalRequest.java
│       │   │   ├── RetrievalResponse.java
│       │   │   └── Citation.java
│       │   └── exception/
│       │       └── RagException.java
│       └── resources/
│           └── application.yml
├── test-docs/                  # Sample documents for testing
│   ├── aws-well-architected.md
│   ├── bedrock-pricing.md
│   └── lambda-best-practices.md
├── scripts/
│   ├── sync-knowledge-base.sh
│   └── test-queries.sh
├── pom.xml
└── README.md
```

---

### Phase 1: Terraform Infrastructure

#### Key Resources

**1. S3 Bucket (s3.tf)**
```hcl
resource "aws_s3_bucket" "documents" {
  bucket = "${var.project_name}-docs-${var.environment}"

  tags = {
    Project = var.project_name
    POC     = "02"
  }
}

resource "aws_s3_bucket_versioning" "documents" {
  bucket = aws_s3_bucket.documents.id
  versioning_configuration {
    status = "Enabled"
  }
}
```

**2. IAM Role (iam.tf)**
```hcl
resource "aws_iam_role" "kb_role" {
  name = "${var.project_name}-kb-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action = "sts:AssumeRole"
      Effect = "Allow"
      Principal = {
        Service = "bedrock.amazonaws.com"
      }
      Condition = {
        StringEquals = {
          "aws:SourceAccount" = data.aws_caller_identity.current.account_id
        }
      }
    }]
  })
}

resource "aws_iam_role_policy" "kb_policy" {
  name = "${var.project_name}-kb-policy"
  role = aws_iam_role.kb_role.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "s3:GetObject",
          "s3:ListBucket"
        ]
        Resource = [
          aws_s3_bucket.documents.arn,
          "${aws_s3_bucket.documents.arn}/*"
        ]
      },
      {
        Effect = "Allow"
        Action = [
          "bedrock:InvokeModel"
        ]
        Resource = [
          "arn:aws:bedrock:${var.region}::foundation-model/amazon.titan-embed-text-v2:0"
        ]
      }
    ]
  })
}
```

**3. Knowledge Base (knowledge-base.tf)**
```hcl
resource "aws_bedrockagent_knowledge_base" "main" {
  name     = "${var.project_name}-kb"
  role_arn = aws_iam_role.kb_role.arn

  knowledge_base_configuration {
    type = "VECTOR"
    vector_knowledge_base_configuration {
      embedding_model_arn = "arn:aws:bedrock:${var.region}::foundation-model/amazon.titan-embed-text-v2:0"
    }
  }

  storage_configuration {
    type = "OPENSEARCH_SERVERLESS"
    opensearch_serverless_configuration {
      collection_arn    = aws_opensearchserverless_collection.kb.arn
      vector_index_name = "bedrock-knowledge-base-index"
      field_mapping {
        vector_field   = "vector"
        text_field     = "text"
        metadata_field = "metadata"
      }
    }
  }

  tags = {
    Project = var.project_name
    POC     = "02"
  }
}

resource "aws_bedrockagent_data_source" "s3" {
  knowledge_base_id = aws_bedrockagent_knowledge_base.main.id
  name              = "s3-docs"

  data_source_configuration {
    type = "S3"
    s3_configuration {
      bucket_arn = aws_s3_bucket.documents.arn
    }
  }

  vector_ingestion_configuration {
    chunking_configuration {
      chunking_strategy = "FIXED_SIZE"
      fixed_size_chunking_configuration {
        max_tokens         = 300
        overlap_percentage = 20
      }
    }
  }
}
```

---

### Phase 2: Java Application

#### Maven Dependencies (pom.xml)
```xml
<dependencies>
    <!-- Spring Boot -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- AWS SDK v2 - Bedrock Agent Runtime -->
    <dependency>
        <groupId>software.amazon.awssdk</groupId>
        <artifactId>bedrockagentruntime</artifactId>
    </dependency>

    <!-- AWS SDK v2 - Bedrock Runtime (for direct model calls) -->
    <dependency>
        <groupId>software.amazon.awssdk</groupId>
        <artifactId>bedrockruntime</artifactId>
    </dependency>

    <!-- Async support -->
    <dependency>
        <groupId>software.amazon.awssdk</groupId>
        <artifactId>netty-nio-client</artifactId>
    </dependency>
</dependencies>

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>software.amazon.awssdk</groupId>
            <artifactId>bom</artifactId>
            <version>2.29.0</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

#### Core Services

**RetrievalService.java** - Direct chunk retrieval
```java
@Service
public class RetrievalService {

    private final BedrockAgentRuntimeAsyncClient client;
    private final String knowledgeBaseId;

    public CompletableFuture<List<RetrievalResult>> retrieve(
            String query,
            int numberOfResults,
            RetrievalFilter filter) {

        var request = RetrieveRequest.builder()
            .knowledgeBaseId(knowledgeBaseId)
            .retrievalQuery(q -> q.text(query))
            .retrievalConfiguration(config -> config
                .vectorSearchConfiguration(vs -> vs
                    .numberOfResults(numberOfResults)
                    .filter(filter)
                    .overrideSearchType(SearchType.HYBRID)))  // Semantic + keyword
            .build();

        return client.retrieve(request)
            .thenApply(RetrieveResponse::retrievalResults);
    }
}
```

**RagService.java** - End-to-end RAG
```java
@Service
public class RagService {

    private final BedrockAgentRuntimeAsyncClient client;
    private final String knowledgeBaseId;
    private final String modelArn;  // Claude 3 Sonnet

    public CompletableFuture<RagResponse> generate(String query) {

        var request = RetrieveAndGenerateRequest.builder()
            .input(i -> i.text(query))
            .retrieveAndGenerateConfiguration(config -> config
                .type(RetrieveAndGenerateType.KNOWLEDGE_BASE)
                .knowledgeBaseConfiguration(kb -> kb
                    .knowledgeBaseId(knowledgeBaseId)
                    .modelArn(modelArn)
                    .retrievalConfiguration(r -> r
                        .vectorSearchConfiguration(vs -> vs
                            .numberOfResults(5)))
                    .generationConfiguration(g -> g
                        .inferenceConfig(ic -> ic
                            .textInferenceConfig(t -> t
                                .temperature(0.0f)
                                .maxTokens(1024))))))
            .build();

        return client.retrieveAndGenerate(request)
            .thenApply(response -> RagResponse.builder()
                .answer(response.output().text())
                .citations(extractCitations(response))
                .build());
    }

    private List<Citation> extractCitations(RetrieveAndGenerateResponse response) {
        return response.citations().stream()
            .flatMap(c -> c.retrievedReferences().stream())
            .map(ref -> Citation.builder()
                .text(ref.content().text())
                .source(ref.location().s3Location().uri())
                .score(ref.metadata().getOrDefault("score", "N/A"))
                .build())
            .toList();
    }
}
```

**EvaluationService.java** - RAG quality metrics
```java
@Service
public class EvaluationService {

    private final BedrockRuntimeAsyncClient bedrockClient;

    /**
     * Calculate relevance score: How relevant are retrieved chunks to the query?
     * Uses LLM-as-judge pattern.
     */
    public CompletableFuture<Double> calculateRelevance(
            String query,
            List<String> retrievedChunks) {

        String prompt = """
            Rate the relevance of these text chunks to the query on a scale of 0-10.

            Query: %s

            Chunks:
            %s

            Return ONLY a number between 0 and 10.
            """.formatted(query, String.join("\n---\n", retrievedChunks));

        return invokeClaude(prompt)
            .thenApply(response -> Double.parseDouble(response.trim()) / 10.0);
    }

    /**
     * Calculate groundedness: Is the answer supported by the retrieved chunks?
     */
    public CompletableFuture<Double> calculateGroundedness(
            String answer,
            List<String> retrievedChunks) {

        String prompt = """
            Rate how well the answer is grounded in (supported by) the provided chunks.
            Score 0-10 where 10 means fully supported, 0 means fabricated.

            Answer: %s

            Source Chunks:
            %s

            Return ONLY a number between 0 and 10.
            """.formatted(answer, String.join("\n---\n", retrievedChunks));

        return invokeClaude(prompt)
            .thenApply(response -> Double.parseDouble(response.trim()) / 10.0);
    }
}
```

---

### Phase 3: Experiments

#### Experiment 1: Chunking Strategy Comparison

**Objective:** Compare retrieval quality across chunking strategies using the same document set.

**Setup:**
1. Create 3 Knowledge Bases with different chunking (Fixed, Hierarchical, Semantic)
2. Upload identical test documents to each
3. Run same 10 test queries against each KB
4. Measure: Relevance score, Response latency, Chunk count

**Expected Results:**
| Strategy | Avg Relevance | Avg Latency | Use Case |
|----------|--------------|-------------|----------|
| Fixed (300) | 0.75 | 120ms | General purpose |
| Hierarchical | 0.82 | 150ms | Structured docs |
| Semantic | 0.85 | 200ms | Narratives |

#### Experiment 2: Retrieval Tuning

**Objective:** Find optimal `numberOfResults` for your use case.

**Variables:**
- numberOfResults: 3, 5, 10, 20
- searchType: SEMANTIC, HYBRID

**Metrics:**
- Relevance@K (are top K results relevant?)
- Answer quality (human eval)
- Token cost (more chunks = more input tokens)

#### Experiment 3: Metadata Filtering

**Objective:** Test precision improvement with metadata filters.

**Setup:**
```java
// Filter by document category
RetrievalFilter filter = RetrievalFilter.builder()
    .equals(FilterAttribute.builder()
        .key("category")
        .value(AttributeValue.builder().stringValue("security").build())
        .build())
    .build();
```

**Document metadata (in S3):**
```json
{
  "metadataAttributes": {
    "category": "security",
    "year": 2024,
    "author": "aws"
  }
}
```

---

## Test Scenarios

### Scenario 1: Basic Retrieval
```bash
curl -X POST http://localhost:8080/api/v1/retrieve \
  -H "Content-Type: application/json" \
  -d '{"query": "What are the pillars of AWS Well-Architected Framework?"}'
```

**Expected:** Returns 5 relevant chunks from well-architected docs.

### Scenario 2: RAG with Citations
```bash
curl -X POST http://localhost:8080/api/v1/generate \
  -H "Content-Type: application/json" \
  -d '{"query": "How should I optimize Lambda cold starts?"}'
```

**Expected:** Returns generated answer with source citations.

### Scenario 3: Filtered Retrieval
```bash
curl -X POST http://localhost:8080/api/v1/retrieve \
  -H "Content-Type: application/json" \
  -d '{
    "query": "What are the security best practices?",
    "filter": {"category": "security"}
  }'
```

**Expected:** Returns only chunks from security-tagged documents.

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

**Comparison:** Self-managed OpenSearch Serverless = $700+/month minimum.

---

## Troubleshooting

| Issue | Cause | Solution |
|-------|-------|----------|
| KB sync fails | IAM permissions | Check role has s3:GetObject |
| Empty retrieval results | Sync not complete | Wait for ingestion job to finish |
| Low relevance scores | Wrong chunking | Try Hierarchical for structured docs |
| High latency | Cold start | Knowledge Base scales down after inactivity |
| "Model not available" | Region mismatch | Verify model ARN region matches KB region |

---

## Success Criteria

- [ ] Terraform deploys without errors
- [ ] Knowledge Base syncs documents successfully
- [ ] Retrieve API returns relevant chunks (relevance > 0.7)
- [ ] RetrieveAndGenerate produces grounded answers
- [ ] Citations correctly reference source documents
- [ ] Can explain chunking trade-offs for exam
- [ ] Metadata filtering narrows results correctly
- [ ] Evaluation service calculates relevance/groundedness

---

## AI Builder Instructions

```
You are implementing POC-02: RAG Pipeline with Bedrock Knowledge Bases.

PHASE 1 - INFRASTRUCTURE (Do this first)
=========================================
1. Create terraform/main.tf:
   - AWS provider with region variable
   - Required providers: aws (~> 5.0), awscc (~> 0.70)

2. Create terraform/variables.tf:
   - project_name (default: "poc02-rag")
   - environment (default: "dev")
   - region (default: "us-east-1")

3. Create terraform/s3.tf:
   - S3 bucket for documents with versioning
   - Block public access

4. Create terraform/iam.tf:
   - IAM role for Knowledge Base with trust policy for bedrock.amazonaws.com
   - Policy: s3:GetObject, s3:ListBucket, bedrock:InvokeModel

5. Create terraform/knowledge-base.tf:
   - aws_bedrockagent_knowledge_base with:
     - Titan Embeddings V2
     - Managed OpenSearch Serverless storage
   - aws_bedrockagent_data_source pointing to S3
     - FIXED_SIZE chunking (300 tokens, 20% overlap)

6. Create terraform/outputs.tf:
   - knowledge_base_id
   - data_source_id
   - bucket_name
   - bucket_arn

PHASE 2 - JAVA APPLICATION
==========================
1. Create pom.xml with:
   - Spring Boot 3.4 parent
   - AWS SDK v2 BOM 2.29+
   - bedrockagentruntime, bedrockruntime, netty-nio-client

2. Create BedrockConfig.java:
   - BedrockAgentRuntimeAsyncClient bean
   - BedrockRuntimeAsyncClient bean
   - Read knowledgeBaseId from application.yml

3. Create RetrievalService.java:
   - retrieve(query, numberOfResults, filter) method
   - Use HYBRID search type
   - Return List<RetrievalResult>

4. Create RagService.java:
   - generate(query) method using RetrieveAndGenerate
   - Extract citations from response
   - Use Claude 3 Sonnet with temperature 0

5. Create EvaluationService.java:
   - calculateRelevance(query, chunks) - LLM-as-judge
   - calculateGroundedness(answer, chunks) - LLM-as-judge

6. Create RagController.java:
   - POST /api/v1/retrieve
   - POST /api/v1/generate
   - POST /api/v1/evaluate

7. Create application.yml:
   - aws.region
   - aws.bedrock.knowledge-base-id (from Terraform output)
   - aws.bedrock.model-id: anthropic.claude-3-sonnet-20240229-v1:0

PHASE 3 - TESTING
=================
1. Upload test documents to S3:
   - Use provided test-docs/ or create sample markdown files
   - Include metadata files (.metadata.json) for filtering tests

2. Trigger KB sync:
   aws bedrock-agent start-ingestion-job --knowledge-base-id <id> --data-source-id <id>

3. Run test queries and verify:
   - Chunks are relevant (score > 0.7)
   - Answers are grounded in sources
   - Citations point to correct documents

IMPORTANT NOTES
===============
- Use async clients (CompletableFuture) for all Bedrock calls
- Temperature 0 for reproducible results in evaluation
- HYBRID search combines semantic + keyword for better recall
- Always include error handling for Bedrock throttling
- Log token usage for cost tracking
```

---

## References

- [Amazon Bedrock Knowledge Bases](https://docs.aws.amazon.com/bedrock/latest/userguide/knowledge-base.html)
- [Chunking and Parsing Configuration](https://docs.aws.amazon.com/bedrock/latest/userguide/kb-chunking-parsing.html)
- [RetrieveAndGenerate API Reference](https://docs.aws.amazon.com/bedrock/latest/APIReference/API_agent-runtime_RetrieveAndGenerate.html)
- [Terraform AWS Bedrock Agent Resources](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/bedrockagent_knowledge_base)
- [AWS SDK for Java v2 - Bedrock](https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/bedrockagentruntime/package-summary.html)
