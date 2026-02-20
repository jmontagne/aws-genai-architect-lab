"""Shared pytest fixtures -- mock Bedrock API responses and sample data."""

import pytest
from unittest.mock import MagicMock


# --- Sample Ground Truth Data ---

SAMPLE_SUMMARIZATION = {
    "id": "sum-01",
    "task_type": "summarization",
    "prompt": "Summarize the following text about Amazon Bedrock.",
    "reference_answer": "Amazon Bedrock is a managed AWS service providing access to multiple foundation models via a single API.",
    "generated_answer": "Amazon Bedrock is a fully managed AWS service that provides access to foundation models through a unified API.",
    "context": "Amazon Bedrock is a fully managed service that makes foundation models available through a unified API.",
}

SAMPLE_TRANSLATION = {
    "id": "trans-01",
    "task_type": "translation",
    "prompt": "Translate to French: The model evaluation requires automated metrics.",
    "reference_answer": "L'evaluation du modele necessite des metriques automatisees.",
    "generated_answer": "L'evaluation des modeles necessite des metriques automatisees.",
    "context": "",
}

SAMPLE_QA = {
    "id": "qa-01",
    "task_type": "question_answering",
    "prompt": "What is the main purpose of Amazon Bedrock Guardrails?",
    "reference_answer": "Guardrails provide configurable safety filters for content policies.",
    "generated_answer": "Bedrock Guardrails are safety filters that check prompts and responses against policies.",
    "context": "Amazon Bedrock Guardrails are configurable safety filters that enforce content policies.",
}

SAMPLE_HALLUCINATION = {
    "id": "hal-01",
    "task_type": "hallucination_detection",
    "prompt": "Based on the context, what AI services does AWS offer?",
    "reference_answer": "AWS offers Bedrock, SageMaker, and Rekognition.",
    "generated_answer": "AWS offers Bedrock, SageMaker, Rekognition, and Quantum Computing for AI.",
    "context": "AWS provides Bedrock for foundation models, SageMaker for ML, and Rekognition for images.",
}


# --- Mock Bedrock Evaluation Job Responses ---

CREATE_EVALUATION_JOB_RESPONSE = {
    "jobArn": "arn:aws:bedrock:us-east-1:123456789012:evaluation-job/poc05-auto-eval",
    "ResponseMetadata": {"HTTPStatusCode": 200},
}

GET_EVALUATION_JOB_COMPLETED_RESPONSE = {
    "jobName": "poc05-auto-eval-demo",
    "jobArn": "arn:aws:bedrock:us-east-1:123456789012:evaluation-job/poc05-auto-eval",
    "status": "Completed",
    "creationTime": "2026-01-15T10:00:00Z",
    "lastModifiedTime": "2026-01-15T10:15:00Z",
    "roleArn": "arn:aws:iam::123456789012:role/poc05-eval-role",
    "outputDataConfig": {
        "s3Uri": "s3://poc05-evaluation-bucket/results/auto-eval/"
    },
    "ResponseMetadata": {"HTTPStatusCode": 200},
}

GET_EVALUATION_JOB_IN_PROGRESS_RESPONSE = {
    "jobName": "poc05-auto-eval-demo",
    "jobArn": "arn:aws:bedrock:us-east-1:123456789012:evaluation-job/poc05-auto-eval",
    "status": "InProgress",
    "creationTime": "2026-01-15T10:00:00Z",
    "ResponseMetadata": {"HTTPStatusCode": 200},
}

GET_EVALUATION_JOB_FAILED_RESPONSE = {
    "jobName": "poc05-auto-eval-demo",
    "jobArn": "arn:aws:bedrock:us-east-1:123456789012:evaluation-job/poc05-auto-eval",
    "status": "Failed",
    "failureMessages": ["Dataset format invalid: missing 'prompt' field"],
    "ResponseMetadata": {"HTTPStatusCode": 200},
}

LIST_EVALUATION_JOBS_RESPONSE = {
    "jobSummaries": [
        {
            "jobName": "poc05-auto-eval-demo",
            "jobArn": "arn:aws:bedrock:us-east-1:123456789012:evaluation-job/abc",
            "status": "Completed",
            "jobType": "Automated",
            "creationTime": "2026-01-15T10:00:00Z",
        },
        {
            "jobName": "poc05-judge-eval-demo",
            "jobArn": "arn:aws:bedrock:us-east-1:123456789012:evaluation-job/def",
            "status": "InProgress",
            "jobType": "Automated",
            "creationTime": "2026-01-15T11:00:00Z",
        },
    ],
    "ResponseMetadata": {"HTTPStatusCode": 200},
}


# --- Fixtures ---


@pytest.fixture
def mock_bedrock_client():
    """Create a mock bedrock client (control plane)."""
    return MagicMock()


@pytest.fixture
def sample_summarization():
    return SAMPLE_SUMMARIZATION.copy()


@pytest.fixture
def sample_translation():
    return SAMPLE_TRANSLATION.copy()


@pytest.fixture
def sample_qa():
    return SAMPLE_QA.copy()


@pytest.fixture
def sample_hallucination():
    return SAMPLE_HALLUCINATION.copy()


@pytest.fixture
def create_job_response():
    return CREATE_EVALUATION_JOB_RESPONSE.copy()


@pytest.fixture
def get_job_completed_response():
    return GET_EVALUATION_JOB_COMPLETED_RESPONSE.copy()


@pytest.fixture
def get_job_in_progress_response():
    return GET_EVALUATION_JOB_IN_PROGRESS_RESPONSE.copy()


@pytest.fixture
def get_job_failed_response():
    return GET_EVALUATION_JOB_FAILED_RESPONSE.copy()


@pytest.fixture
def list_jobs_response():
    return LIST_EVALUATION_JOBS_RESPONSE.copy()
