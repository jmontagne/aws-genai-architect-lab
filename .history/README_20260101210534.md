# 🧪 AWS GenAI Architect Lab

> **"One Concept, One POC."**
> A collection of rapid prototypes, architectural experiments, and exam preparation labs for the **AWS Certified Generative AI Developer - Professional** (AIP-C01).

**Owner:** [Jacques Montagne](https://github.com/jmontagne)
**Status:** `Active Learning`

---

## 🎯 The Philosophy
This repository is NOT a monolithic application.
It is a **Laboratory** where I isolate specific AWS GenAI concepts to understand them deeply without the noise of a full-blown framework.

* **Speed over Polish:** Code is designed to verify a hypothesis, not to be production-perfect.
* **Isolation:** Each `POC-xx` folder is a standalone project.
* **Exam Focus:** Every POC maps directly to a specific domain in the AWS Exam Guide.

---

## 🔬 The Experiments (Syllabus)

| ID | Domain | Experiment Name & Goal | Stack | Status |
| :--- | :--- | :--- | :--- | :--- |
| **POC-01** | *Fundamentals* | **Bedrock Connection & Parameters**<br>Test raw SDK latency and inference parameters (Temp, Top-P). | Java SDK v2 | 🔄 Pending |
| **POC-02** | *Architecture* | **The RAG Pipeline**<br>Manual implementation of Chunking + Embeddings vs. Knowledge Bases. | LangChain4j | ⏳ Planned |
| **POC-03** | *Agents* | **SQL Agent Safety**<br>Building an agent that queries a database. Can we prevent it from dropping tables? | Java / Tools | ⏳ Planned |
| **POC-04** | *Security* | **PII Guardrails**<br>Testing Amazon Bedrock Guardrails with real PII datasets (GDPR focus). | Python Script | ⏳ Planned |
| **POC-05** | *Evaluation* | **LLM-as-a-Judge**<br>Automated scoring of RAG retrieval quality using Ragas. | Python / Ragas | ⏳ Planned |

---

## 🛠️ Tech Stack Strategy

I use a hybrid approach to simulate a real Enterprise Environment:

* **Application Logic (The "Build"):**
    * **Java 21 + Spring Boot 3:** Because this is what runs in Production (Banking/Insurance).
    * **LangChain4j:** The standard for Java-based AI integration.

* **Operations & Evaluation (The "Test"):**
    * **Python 3.12 + Boto3:** For quick scripting, infrastructure setup, and using data science tools like Ragas.

---

## 📂 Repository Structure

```text
aws-genai-architect-lab/
├── README.md                   # This file
├── .gitignore                  # Global ignore rules
├── POC-01-bedrock-basics/      # Independent Maven/Gradle project
│   ├── src/
│   └── pom.xml
├── POC-02-rag-pipeline/        # Independent project
└── scripts/                    # Shared Python utility scripts
```


## 📜 Disclaimer
This code is for educational purposes and exam preparation. It focuses on clarity and mechanics rather than production-grade error handling.
