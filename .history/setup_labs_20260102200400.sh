#!/bin/bash

# Lab Setup Script for Jacques Montagne - AWS GenAI Architect Portfolio
# This script creates 7 POC directories with detailed technical specifications.

projects=(
    "POC-01-bedrock-basics"
    "POC-02-rag-pipeline"
    "POC-03-agentic-tools"
    "POC-04-security-guardrails"
    "POC-05-automated-evaluation"
    "POC-06-multi-modal-vision"
    "POC-07-model-customization"
)

for project in "${projects[@]}"; do
    mkdir -p "$project/terraform"
    mkdir -p "$project/src"
    
    cat <<EOF > "$project/README.md"
# $project Specification

## 🎯 Objective
Master the technical implementation of $(echo $project | sed 's/POC-[0-9]*-//' | tr '-' ' ') within the AWS ecosystem, focusing on enterprise-grade patterns.

## 🏗️ Architecture Requirements
- **Infrastructure (IaC):** Terraform. Provision all necessary IAM roles, Bedrock permissions, and storage.
- **Application Logic:** Java 21 / Spring Boot 3 / LangChain4j. Ensure strong typing and clean architecture.
- **Utility & Ops:** Python 3.12 / Boto3 for testing and deployment scripts.

## 📝 Technical Deep Dive
$(case $project in
    "POC-01-bedrock-basics")
        echo "### Scope:
- Implement a Java client using 'BedrockRuntimeClient' (v2 SDK).
- Target Models: Claude 3.5 Sonnet & Titan Text G1 - Premier.
- Terraform: Create an IAM User with 'AmazonBedrockFullAccess' and setup provider.
- Learning Focus: Difference between 'InvokeModel' and 'InvokeModelWithResponseStream'. Manipulation of Temperature, Top-P, and Max Tokens."
        ;;
    "POC-02-rag-pipeline")
        echo "### Scope:
- Build a Knowledge Base for Amazon Bedrock using Terraform.
- Data Ingestion: S3 bucket -> Lambda Trigger -> Vector Ingestion.
- Vector Store: Amazon OpenSearch Serverless (OSS).
- Java Logic: Implement a 'Retrieval' service that queries the KB and augments the prompt.
- Learning Focus: Chunking strategies (Fixed vs. Hierarchical) and Embedding models (Titan V2)."
        ;;
    "POC-03-agentic-tools")
        echo "### Scope:
- Create an Agent for Amazon Bedrock using Action Groups.
- Tool Use: Implement a Java Lambda function that queries a mock SQL Database.
- OpenAPI Schema: Define the tool contract in JSON/YAML.
- Learning Focus: ReAct (Reason + Act) prompting strategy and Agent session management."
        ;;
    "POC-04-security-guardrails")
        echo "### Scope:
- Provision 'Amazon Bedrock Guardrails' via Terraform.
- Filters: Implement PII detection (SSN, Emails), profanity filters, and custom denied topics.
- Validation Script: Python script to send 'poisoned' prompts and verify Guardrail intervention.
- Learning Focus: Difference between 'Masking' and 'Blocking' sensitive data."
        ;;
    "POC-05-automated-evaluation")
        echo "### Scope:
- Setup an Evaluation Pipeline using the 'LLM-as-a-Judge' pattern.
- Tooling: Python with 'Ragas' or 'AWS Bedrock Model Evaluation'.
- Metrics: Measure 'Faithfulness' (hallucination check) and 'Answer Relevance'.
- Learning Focus: Creating 'Ground Truth' datasets and interpreting evaluation scores."
        ;;
    "POC-06-multi-modal-vision")
        echo "### Scope:
- Multimodal Processing: Use Claude 3.5 Sonnet Vision capabilities.
- Task: Analyze an image (e.g., insurance claim photo) and extract a structured JSON report.
- Java Logic: Handle Base64 image encoding and multi-part messages in the SDK.
- Learning Focus: Token costs for images vs. text and prompt engineering for visual reasoning."
        ;;
    "POC-07-model-customization")
        echo "### Scope:
- Fine-tuning Workflow: Prepare a JSONL training dataset in S3.
- Automation: Python script to trigger a 'Bedrock Fine-tuning Job'.
- Evaluation: Compare the fine-tuned model against the base model.
- Learning Focus: Hyperparameters (Epochs, Batch Size) and when to choose Fine-tuning over RAG."
        ;;
esac)

## 🤖 AI Prompt Instruction (For Kilo Code/Copilot)
"Acting as an AWS GenAI Architect, generate the implementation for this POC. 
1. Start with the Terraform files in /terraform to establish IAM and Bedrock access.
2. Provide the Java Maven configuration (pom.xml) with AWS SDK v2.
3. Write the Java 21 implementation in /src focusing on readability and exam-relevant methods.
4. Explain how this specific implementation addresses the 'Safe Modernization' strategy."
EOF

    echo "✅ [SUCCESS] Created $project with detailed README."
done

echo "--------------------------------------------------"
echo "🚀 Lab is ready. Your journey to AWS GenAI Pro starts now."