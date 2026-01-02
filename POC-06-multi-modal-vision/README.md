# POC-06-multi-modal-vision Specification

## 🎯 Objective
Master the technical implementation of multi modal vision within the AWS ecosystem, focusing on enterprise-grade patterns.

## 🏗️ Architecture Requirements
- **Infrastructure (IaC):** Terraform. Provision all necessary IAM roles, Bedrock permissions, and storage.
- **Application Logic:** Java 21 / Spring Boot 3 / LangChain4j. Ensure strong typing and clean architecture.
- **Utility & Ops:** Python 3.12 / Boto3 for testing and deployment scripts.

## 📝 Technical Deep Dive
### Scope:
- Multimodal Processing: Use Claude 3.5 Sonnet Vision capabilities.
- Task: Analyze an image (e.g., insurance claim photo) and extract a structured JSON report.
- Java Logic: Handle Base64 image encoding and multi-part messages in the SDK.
- Learning Focus: Token costs for images vs. text and prompt engineering for visual reasoning.

## 🤖 AI Prompt Instruction (For Kilo Code/Copilot)
"Acting as an AWS GenAI Architect, generate the implementation for this POC. 
1. Start with the Terraform files in /terraform to establish IAM and Bedrock access.
2. Provide the Java Maven configuration (pom.xml) with AWS SDK v2.
3. Write the Java 21 implementation in /src focusing on readability and exam-relevant methods.
4. Explain how this specific implementation addresses the 'Safe Modernization' strategy."
