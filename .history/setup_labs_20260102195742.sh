#!/bin/bash

# Główne laboratorium Jacques'a Montagne - Edycja Strategiczna 2026
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
    
    # Tworzenie specyfikacji dla AI
    cat <<EOF > "$project/README.md"
# $project

## 🎯 Project Goal
This POC is part of the AWS GenAI Architect Lab. The goal is to isolate and master specific mechanics of the AWS GenAI Professional domain.

## 🏗️ Architecture Requirements
- **Logic:** Java 21 / LangChain4j (where applicable)[cite: 44, 49].
- **Infrastructure:** Terraform (IaC) for all AWS resources[cite: 60].
- **Validation:** Python / Boto3 for testing and AWS interaction scripts[cite: 45, 51].

## 📝 POC Specifics
$(case $project in
    "POC-01-bedrock-basics")
        echo "Goal: Establish raw SDK connection to Amazon Bedrock.
Task: Invoke Claude 3.5 Sonnet and Titan models.
Focus: Understanding Inference Parameters (Temperature, Top-P, Stop Sequences)[cite: 47]."
        ;;
    "POC-02-rag-pipeline")
        echo "Goal: Implement Retrieval-Augmented Generation.
Task: Create a pipeline: Document -> S3 -> Embedding (Titan V2) -> Vector Store.
Focus: Chunking strategies and Knowledge Bases for Amazon Bedrock[cite: 48]."
        ;;
    "POC-03-agentic-tools")
        echo "Goal: Build an autonomous Agent with Tool Use.
Task: Agent must query a SQL database (simulated) to answer business questions.
Focus: ReAct prompting and Action Groups[cite: 49]."
        ;;
    "POC-04-security-guardrails")
        echo "Goal: Hardening the AI application.
Task: Implement Guardrails for Amazon Bedrock to filter PII (GDPR compliance).
Focus: Content filtering and sensitive data masking[cite: 50]."
        ;;
    "POC-05-automated-evaluation")
        echo "Goal: Establish an Evaluation framework.
Task: Use LLM-as-a-Judge pattern to score RAG outputs.
Focus: Metrics like Faithfulness, Answer Relevance using Ragas (Python)[cite: 51]."
        ;;
    "POC-06-multi-modal-vision")
        echo "Goal: Analyze non-textual data.
Task: Process claim images (car accidents) using Claude 3.5 Sonnet Vision.
Focus: Image-to-text extraction and visual reasoning."
        ;;
    "POC-07-model-customization")
        echo "Goal: Beyond RAG.
Task: Prepare a JSONL dataset and trigger a Bedrock Fine-tuning job.
Focus: When to use fine-tuning vs. RAG."
        ;;
esac)

## 🤖 AI Prompt Instruction
"Based on this README, generate a production-grade implementation.
1. Use Terraform for IAM roles, S3 buckets, and Bedrock configurations.
2. Use Java/Maven for the application code.
3. Explain the 'Why' behind every architectural choice for exam preparation."
EOF

    echo "✅ Created directory and README for $project"
done

echo "🚀 Laboratory structure is ready for Deep Work."