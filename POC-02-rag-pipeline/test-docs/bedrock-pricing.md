# Amazon Bedrock Pricing Guide

## Overview

Amazon Bedrock offers a serverless experience with no upfront costs. You pay only for what you use, with pricing based on the number of input and output tokens processed.

## Foundation Model Pricing

### Anthropic Claude Models

| Model | Input (per 1K tokens) | Output (per 1K tokens) |
|-------|----------------------|------------------------|
| Claude 3 Opus | $0.015 | $0.075 |
| Claude 3 Sonnet | $0.003 | $0.015 |
| Claude 3 Haiku | $0.00025 | $0.00125 |
| Claude 3.5 Sonnet | $0.003 | $0.015 |

### Amazon Titan Models

| Model | Input (per 1K tokens) | Output (per 1K tokens) |
|-------|----------------------|------------------------|
| Titan Text Express | $0.0002 | $0.0006 |
| Titan Text Lite | $0.00015 | $0.0002 |
| Titan Text Premier | $0.0005 | $0.0015 |

### Amazon Titan Embeddings

| Model | Pricing |
|-------|---------|
| Titan Embeddings V2 | $0.00002 per 1K tokens |
| Titan Multimodal Embeddings | $0.0008 per image, $0.00002 per 1K text tokens |

## Knowledge Bases Pricing

Amazon Bedrock Knowledge Bases has the following cost components:

### Vector Store Options

**1. Managed OpenSearch Serverless (Included with Knowledge Bases)**
- No additional cost when using the managed vector store
- Included in Knowledge Base pricing
- Automatic scaling

**2. Bring Your Own Vector Store**
- OpenSearch Serverless: Starting at $0.24 per OCU-hour
- Amazon Aurora PostgreSQL: Standard Aurora pricing
- Pinecone: Separate pricing from Pinecone
- Redis Enterprise Cloud: Separate pricing from Redis

### Data Ingestion

- Embedding generation: Based on Titan Embeddings pricing
- Document processing: No additional charge
- Sync operations: No additional charge

### Retrieval and Generation

- Retrieve API: No additional charge (embedding costs apply)
- RetrieveAndGenerate API: Foundation model costs apply

## Provisioned Throughput

For consistent, high-volume workloads, Provisioned Throughput offers:

- Guaranteed model units
- Lower per-token costs at scale
- 1-month or 6-month commitment options

### Commitment Pricing

| Duration | Discount |
|----------|----------|
| 1-month | Up to 50% savings |
| 6-month | Up to 75% savings |

## Cost Optimization Tips

### 1. Choose the Right Model
- Use Haiku for simple tasks (classification, extraction)
- Use Sonnet for balanced performance
- Reserve Opus for complex reasoning tasks

### 2. Optimize Token Usage
- Use concise prompts
- Implement prompt caching where supported
- Set appropriate max_tokens limits

### 3. Use Knowledge Bases Efficiently
- Use managed vector store to avoid OpenSearch costs
- Optimize chunk sizes (300-500 tokens recommended)
- Use metadata filtering to reduce retrieval volume

### 4. Monitor and Alert
- Set up AWS Cost Explorer alerts
- Track token usage with CloudWatch
- Review usage patterns monthly

## Example Monthly Cost Estimates

### Small RAG Application
- 10,000 queries/month
- 5 chunks retrieved per query
- Claude 3 Sonnet for generation

Estimated cost:
- Embeddings: ~$2
- Generation: ~$45
- **Total: ~$47/month**

### Medium RAG Application
- 100,000 queries/month
- 10 chunks retrieved per query
- Claude 3 Haiku for generation

Estimated cost:
- Embeddings: ~$20
- Generation: ~$75
- **Total: ~$95/month**

## Free Tier

Amazon Bedrock offers a free tier for new users:
- First 2 months
- Limited to specific models
- Subject to usage caps

Check the AWS Free Tier page for current offers and limitations.
