# POC-01-bedrock-basics Specification

## 🎯 Objective
Master the technical implementation of bedrock basics within the AWS ecosystem, focusing on enterprise-grade patterns.

## 🏗️ Architecture Requirements
- **Infrastructure (IaC):** Terraform. Provision all necessary IAM roles, Bedrock permissions, and storage.
- **Application Logic:** Java 21 / Spring Boot 3 / LangChain4j. Ensure strong typing and clean architecture.
- **Utility & Ops:** Python 3.12 / Boto3 for testing and deployment scripts.

## 📝 Technical Deep Dive
### Scope:
- Implement a Java client using 'BedrockRuntimeClient' (v2 SDK).
- Target Models: Claude 3.5 Sonnet & Titan Text G1 - Premier.
- Terraform: Create an IAM User with 'AmazonBedrockFullAccess' and setup provider.
- Learning Focus: Difference between 'InvokeModel' and 'InvokeModelWithResponseStream'. Manipulation of Temperature, Top-P, and Max Tokens.

## 🤖 AI Prompt Instruction (For Kilo Code/Copilot)
"Acting as an AWS GenAI Architect, generate the implementation for this POC. 
1. Start with the Terraform files in /terraform to establish IAM and Bedrock access.
2. Provide the Java Maven configuration (pom.xml) with AWS SDK v2.
3. Write the Java 21 implementation in /src focusing on readability and exam-relevant methods.
4. Explain how this specific implementation addresses the 'Safe Modernization' strategy."
