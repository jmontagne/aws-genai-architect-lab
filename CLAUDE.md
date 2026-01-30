# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Language Rules

- **Conversation:** Polish (user prefers Polish communication)
- **All files & code:** English only
- **Git commits:** English only (no other languages allowed in commit messages)

## Project Overview

AWS GenAI Architect Lab - A collection of isolated POCs for AWS Certified Generative AI Developer Professional (AIP-C01) exam preparation. Each POC explores a specific AWS GenAI concept with its own infrastructure.

**Philosophy:** "One Concept, One POC. Infrastructure as Code by Default."

## Tech Stack

- **Application:** Java 21 / Spring Boot 3.4 / LangChain4j (POC-01, 02, 03, 06)
- **Operations/Evaluation:** Python 3.12 / Boto3 / Ragas (POC-04, 05, 07)
- **Infrastructure:** Terraform (all POCs)
- **Runtime:** AWS SDK for Java v2 (Asynchronous Client)

## Build Commands

### Java POCs
```bash
mvn clean compile        # Compile
mvn test                 # Run tests
mvn package              # Build JAR
```

### Python POCs
```bash
pip install -r requirements.txt
pytest
```

### Terraform (per POC)
```bash
cd POC-XX-name/terraform/
terraform init
terraform plan
terraform apply
terraform destroy
```

## Architecture

Each POC is completely independent:
```
POC-XX-name/
├── terraform/           # IaC for AWS resources
├── src/                 # Source code (main + test)
├── scripts/             # Utility scripts
└── README.md            # POC specifications with AI Builder Instructions
```

## POC Status

| POC | Focus | Stack | Status |
|-----|-------|-------|--------|
| POC-01 | Bedrock Inference & Observability | Java SDK v2 | Active |
| POC-02 | RAG Pipeline & Knowledge Bases | LangChain4j | Planned |
| POC-03 | Agentic Tools (SQL queries) | Java/Tools | Planned |
| POC-04 | Security Guardrails (PII/GDPR) | Python/Boto3 | Planned |
| POC-05 | Automated Evaluation (Ragas) | Python/Ragas | Planned |
| POC-06 | Multi-modal Vision | Java/Vision | Planned |
| POC-07 | Model Fine-tuning | Python/CLI | Planned |

## Key Patterns

- **Safe Modernization:** Decouple from LLM providers, implement audit logging, track costs
- **IaC First:** Define Terraform resources before application code
- **No Shared Code:** Each POC is isolated; avoid cross-POC dependencies
- **Each POC README contains AI Builder Instructions** with specific implementation guidance
