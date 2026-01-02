# POC-02-rag-pipeline Specification

## 🎯 Objective
Master the technical implementation of rag pipeline within the AWS ecosystem, focusing on enterprise-grade patterns.

## 🏗️ Architecture Requirements
- **Infrastructure (IaC):** Terraform. Provision all necessary IAM roles, Bedrock permissions, and storage.
- **Application Logic:** Java 21 / Spring Boot 3 / LangChain4j. Ensure strong typing and clean architecture.
- **Utility & Ops:** Python 3.12 / Boto3 for testing and deployment scripts.

## 📝 Technical Deep Dive
### Scope:
- Build a Knowledge Base for Amazon Bedrock using Terraform.
- Data Ingestion: S3 bucket -> Lambda Trigger -> Vector Ingestion.
- Vector Store: Amazon OpenSearch Serverless (OSS).
- Java Logic: Implement a 'Retrieval' service that queries the KB and augments the prompt.
- Learning Focus: Chunking strategies (Fixed vs. Hierarchical) and Embedding models (Titan V2).

## 🤖 AI Prompt Instruction (For Kilo Code/Copilot)
"Acting as an AWS GenAI Architect, generate the implementation for this POC. 
1. Start with the Terraform files in /terraform to establish IAM and Bedrock access.
2. Provide the Java Maven configuration (pom.xml) with AWS SDK v2.
3. Write the Java 21 implementation in /src focusing on readability and exam-relevant methods.
4. Explain how this specific implementation addresses the 'Safe Modernization' strategy."
