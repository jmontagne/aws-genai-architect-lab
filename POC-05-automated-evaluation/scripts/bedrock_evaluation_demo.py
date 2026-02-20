"""
Bedrock Model Evaluation Job -- API Demo (Mocked / Dry-Run)

Demonstrates the Bedrock Model Evaluation Job API structure and workflow.
This script shows the exact API calls needed to create, monitor, and
retrieve results from a Bedrock evaluation job.

Mode: DRY-RUN by default (prints API payloads without making calls).
Set LIVE_MODE=true environment variable to execute real API calls.

Cost: $0 in dry-run mode. Live mode costs depend on model and dataset size.

Exam relevance (AIF-C01):
- Bedrock supports 3 evaluation modes: automatic, human, LLM-as-a-judge
- Automatic evaluation uses programmatic metrics (BERTScore, F1, exact match)
- Evaluation jobs require: S3 dataset, IAM role, model ID, metric config
- No extra charge for evaluation orchestration -- you pay only for model tokens

Usage:
    python scripts/bedrock_evaluation_demo.py                # Dry-run (default)
    LIVE_MODE=true python scripts/bedrock_evaluation_demo.py  # Real API calls
"""

import json
import os

import boto3


REGION = os.environ.get("AWS_REGION", "us-east-1")
LIVE_MODE = os.environ.get("LIVE_MODE", "false").lower() == "true"

# These values come from Terraform outputs
EVALUATION_BUCKET = os.environ.get("EVALUATION_BUCKET", "<FROM_TERRAFORM_OUTPUT>")
EVALUATION_ROLE_ARN = os.environ.get("EVALUATION_ROLE_ARN", "<FROM_TERRAFORM_OUTPUT>")


def build_automatic_evaluation_payload() -> dict:
    """Build payload for an automatic (programmatic) evaluation job.

    Automatic evaluation uses built-in metrics like BERTScore, F1, exact match.
    Cost: Only model inference tokens (no evaluation surcharge).
    """
    return {
        "jobName": "poc05-auto-eval-demo",
        "jobDescription": "Automatic evaluation with programmatic metrics",
        "roleArn": EVALUATION_ROLE_ARN,
        "evaluationConfig": {
            "automated": {
                "datasetMetricConfigs": [
                    {
                        "taskType": "Summarization",
                        "dataset": {
                            "name": "poc05-summarization",
                            "datasetLocation": {
                                "s3Uri": f"s3://{EVALUATION_BUCKET}/datasets/ground_truth.jsonl"
                            },
                        },
                        "metricNames": [
                            "BertScore",
                            "Rouge",
                        ],
                    }
                ]
            }
        },
        "inferenceConfig": {
            "models": [
                {
                    "bedrockModel": {
                        "modelIdentifier": "amazon.nova-micro-v1:0",
                        "inferenceParams": json.dumps(
                            {"temperature": 0.0, "maxTokens": 256}
                        ),
                    }
                }
            ]
        },
        "outputDataConfig": {
            "s3Uri": f"s3://{EVALUATION_BUCKET}/results/auto-eval/"
        },
    }


def build_llm_as_judge_payload() -> dict:
    """Build payload for LLM-as-a-judge evaluation job.

    Uses a separate LLM to evaluate model outputs on dimensions like
    correctness, completeness, and harmfulness.
    Cost: Judge model token costs + evaluated model token costs.
    """
    return {
        "jobName": "poc05-judge-eval-demo",
        "jobDescription": "LLM-as-a-judge evaluation for quality assessment",
        "roleArn": EVALUATION_ROLE_ARN,
        "evaluationConfig": {
            "automated": {
                "datasetMetricConfigs": [
                    {
                        "taskType": "General",
                        "dataset": {
                            "name": "poc05-qa-dataset",
                            "datasetLocation": {
                                "s3Uri": f"s3://{EVALUATION_BUCKET}/datasets/ground_truth.jsonl"
                            },
                        },
                        "metricNames": [
                            "Builtin.Correctness",
                            "Builtin.Completeness",
                            "Builtin.Harmfulness",
                        ],
                    }
                ]
            }
        },
        "inferenceConfig": {
            "models": [
                {
                    "bedrockModel": {
                        "modelIdentifier": "amazon.nova-micro-v1:0",
                        "inferenceParams": json.dumps(
                            {"temperature": 0.0, "maxTokens": 512}
                        ),
                    }
                }
            ]
        },
        "outputDataConfig": {
            "s3Uri": f"s3://{EVALUATION_BUCKET}/results/judge-eval/"
        },
    }


def build_human_evaluation_payload() -> dict:
    """Build payload for human-based evaluation job.

    Uses human workers to rate model outputs.
    Cost: $0.21 per completed human task + model inference tokens.
    """
    return {
        "jobName": "poc05-human-eval-demo",
        "jobDescription": "Human evaluation for subjective quality assessment",
        "roleArn": EVALUATION_ROLE_ARN,
        "evaluationConfig": {
            "human": {
                "humanWorkflowConfig": {
                    "flowDefinitionArn": "<SAGEMAKER_FLOW_DEFINITION_ARN>",
                    "instructions": "Rate the response quality on a scale of 1-5.",
                },
                "datasetMetricConfigs": [
                    {
                        "taskType": "General",
                        "dataset": {
                            "name": "poc05-human-dataset",
                            "datasetLocation": {
                                "s3Uri": f"s3://{EVALUATION_BUCKET}/datasets/ground_truth.jsonl"
                            },
                        },
                        "metricNames": ["HumanRating"],
                    }
                ],
            }
        },
        "inferenceConfig": {
            "models": [
                {
                    "bedrockModel": {
                        "modelIdentifier": "amazon.nova-micro-v1:0",
                        "inferenceParams": json.dumps(
                            {"temperature": 0.0, "maxTokens": 512}
                        ),
                    }
                }
            ]
        },
        "outputDataConfig": {
            "s3Uri": f"s3://{EVALUATION_BUCKET}/results/human-eval/"
        },
    }


def print_payload(title: str, payload: dict):
    """Pretty-print an API payload."""
    print(f"\n{'='*70}")
    print(f"  {title}")
    print(f"{'='*70}")
    print(json.dumps(payload, indent=2))


def demo_get_evaluation_job():
    """Show the structure of a GetModelEvaluationJob response."""
    mock_response = {
        "jobName": "poc05-auto-eval-demo",
        "jobArn": "arn:aws:bedrock:us-east-1:123456789012:evaluation-job/abc123",
        "status": "Completed",
        "creationTime": "2026-01-15T10:00:00Z",
        "lastModifiedTime": "2026-01-15T10:15:00Z",
        "outputDataConfig": {
            "s3Uri": f"s3://{EVALUATION_BUCKET}/results/auto-eval/"
        },
        "evaluationResults": {
            "automated": {
                "datasetMetricResults": [
                    {
                        "taskType": "Summarization",
                        "datasetName": "poc05-summarization",
                        "metricResults": [
                            {"metricName": "BertScore", "value": 0.89},
                            {"metricName": "Rouge", "value": 0.72},
                        ],
                    }
                ]
            }
        },
    }
    print(f"\n{'='*70}")
    print("  MOCK: GetModelEvaluationJob Response")
    print(f"{'='*70}")
    print(json.dumps(mock_response, indent=2))


def main():
    print("=" * 70)
    print("POC-05: Bedrock Model Evaluation Job API Demo")
    print(f"Mode: {'LIVE' if LIVE_MODE else 'DRY-RUN (printing payloads only)'}")
    print(f"Region: {REGION}")
    print("=" * 70)

    # --- Mode 1: Automatic (Programmatic) Evaluation ---
    auto_payload = build_automatic_evaluation_payload()
    print_payload(
        "MODE 1: Automatic Evaluation (Programmatic Metrics)\n"
        "  Metrics: BERTScore, ROUGE\n"
        "  Cost: Model inference tokens only (no evaluation surcharge)",
        auto_payload,
    )

    # --- Mode 2: LLM-as-a-Judge Evaluation ---
    judge_payload = build_llm_as_judge_payload()
    print_payload(
        "MODE 2: LLM-as-a-Judge Evaluation\n"
        "  Metrics: Correctness, Completeness, Harmfulness\n"
        "  Cost: Judge model tokens + evaluated model tokens",
        judge_payload,
    )

    # --- Mode 3: Human-Based Evaluation ---
    human_payload = build_human_evaluation_payload()
    print_payload(
        "MODE 3: Human-Based Evaluation\n"
        "  Metrics: Human rating (1-5 scale)\n"
        "  Cost: $0.21 per task + model inference tokens",
        human_payload,
    )

    # --- Mock: GetEvaluationJob Response ---
    demo_get_evaluation_job()

    if LIVE_MODE:
        print(f"\n{'='*70}")
        print("LIVE MODE: Creating automatic evaluation job...")
        client = boto3.client("bedrock", region_name=REGION)
        try:
            response = client.create_model_evaluation_job(**auto_payload)
            print(f"Job ARN: {response['jobArn']}")
            print("Use 'aws bedrock get-model-evaluation-job' to check status.")
        except Exception as e:
            print(f"ERROR: {e}")
    else:
        print(f"\n{'='*70}")
        print("DRY-RUN complete. Set LIVE_MODE=true to execute real API calls.")

    print(f"\n{'='*70}")
    print("BEDROCK EVALUATION MODES -- EXAM SUMMARY")
    print(f"{'--'*35}")
    print("  Mode                 | Metrics                    | Cost")
    print("  ---------------------|----------------------------|---------------------------")
    print("  Automatic            | BERTScore, ROUGE, F1,      | Inference tokens only")
    print("  (Programmatic)       | Exact Match                | (cheapest)")
    print("  ---------------------|----------------------------|---------------------------")
    print("  LLM-as-a-Judge       | Correctness, Completeness, | Judge + evaluated model")
    print("                       | Harmfulness, Faithfulness  | tokens (moderate)")
    print("  ---------------------|----------------------------|---------------------------")
    print("  Human-Based          | Custom ratings (1-5),      | $0.21/task + inference")
    print("                       | subjective quality         | (most expensive)")
    print(f"{'='*70}")


if __name__ == "__main__":
    main()
