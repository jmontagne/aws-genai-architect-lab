"""Shared pytest fixtures — mock boto3 clients and sample responses."""

import pytest
from unittest.mock import MagicMock


# --- Sample API Responses ---

APPLY_GUARDRAIL_PII_MASK_RESPONSE = {
    "action": "GUARDRAIL_INTERVENED",
    "outputs": [{"text": "My SSN is {SSN} and email is {EMAIL}"}],
    "assessments": [
        {
            "sensitiveInformationPolicy": {
                "piiEntities": [
                    {
                        "type": "US_SOCIAL_SECURITY_NUMBER",
                        "match": "123-45-6789",
                        "action": "ANONYMIZED",
                    },
                    {
                        "type": "EMAIL",
                        "match": "john@example.com",
                        "action": "ANONYMIZED",
                    },
                ]
            }
        }
    ],
    "ResponseMetadata": {"HTTPStatusCode": 200},
}

APPLY_GUARDRAIL_PII_BLOCK_RESPONSE = {
    "action": "GUARDRAIL_INTERVENED",
    "outputs": [
        {"text": "The model response was filtered by security guardrails."}
    ],
    "assessments": [
        {
            "sensitiveInformationPolicy": {
                "piiEntities": [
                    {
                        "type": "US_SOCIAL_SECURITY_NUMBER",
                        "match": "123-45-6789",
                        "action": "BLOCKED",
                    },
                ]
            }
        }
    ],
    "ResponseMetadata": {"HTTPStatusCode": 200},
}

APPLY_GUARDRAIL_CLEAN_RESPONSE = {
    "action": "NONE",
    "outputs": [{"text": "What is the capital of France?"}],
    "assessments": [{}],
    "ResponseMetadata": {"HTTPStatusCode": 200},
}

APPLY_GUARDRAIL_DENIED_TOPIC_RESPONSE = {
    "action": "GUARDRAIL_INTERVENED",
    "outputs": [{"text": "Your request contains content that is not allowed."}],
    "assessments": [
        {
            "topicPolicy": {
                "topics": [
                    {
                        "name": "Investment Advice",
                        "type": "DENY",
                        "action": "BLOCKED",
                    }
                ]
            }
        }
    ],
    "ResponseMetadata": {"HTTPStatusCode": 200},
}

APPLY_GUARDRAIL_CONTENT_FILTER_RESPONSE = {
    "action": "GUARDRAIL_INTERVENED",
    "outputs": [{"text": "Your request contains content that is not allowed."}],
    "assessments": [
        {
            "contentPolicy": {
                "filters": [
                    {
                        "type": "VIOLENCE",
                        "confidence": "HIGH",
                        "action": "BLOCKED",
                    }
                ]
            }
        }
    ],
    "ResponseMetadata": {"HTTPStatusCode": 200},
}

CONVERSE_WITH_GUARDRAIL_RESPONSE = {
    "output": {
        "message": {
            "role": "assistant",
            "content": [{"text": "The capital of France is Paris."}],
        }
    },
    "stopReason": "end_turn",
    "usage": {"inputTokens": 15, "outputTokens": 10, "totalTokens": 25},
    "trace": {
        "guardrail": {
            "inputAssessment": {
                "topicPolicy": {"topics": []},
                "contentPolicy": {"filters": []},
                "wordPolicy": {"customWords": [], "managedWordLists": []},
                "sensitiveInformationPolicy": {"piiEntities": [], "regexes": []},
            },
            "outputAssessments": [
                {
                    "topicPolicy": {"topics": []},
                    "contentPolicy": {"filters": []},
                    "wordPolicy": {"customWords": [], "managedWordLists": []},
                    "sensitiveInformationPolicy": {
                        "piiEntities": [],
                        "regexes": [],
                    },
                }
            ],
        }
    },
    "ResponseMetadata": {"HTTPStatusCode": 200},
}

CONVERSE_GUARDRAIL_INTERVENED_RESPONSE = {
    "output": {
        "message": {
            "role": "assistant",
            "content": [
                {"text": "The model response was filtered by security guardrails."}
            ],
        }
    },
    "stopReason": "guardrail_intervened",
    "usage": {"inputTokens": 20, "outputTokens": 0, "totalTokens": 20},
    "trace": {
        "guardrail": {
            "inputAssessment": {
                "sensitiveInformationPolicy": {
                    "piiEntities": [
                        {
                            "type": "US_SOCIAL_SECURITY_NUMBER",
                            "match": "123-45-6789",
                            "action": "BLOCKED",
                        }
                    ],
                    "regexes": [],
                }
            },
            "outputAssessments": [],
        }
    },
    "ResponseMetadata": {"HTTPStatusCode": 200},
}


# --- Fixtures ---


@pytest.fixture
def mock_bedrock_runtime():
    """Create a mock bedrock-runtime client."""
    return MagicMock()


@pytest.fixture
def pii_mask_response():
    return APPLY_GUARDRAIL_PII_MASK_RESPONSE.copy()


@pytest.fixture
def pii_block_response():
    return APPLY_GUARDRAIL_PII_BLOCK_RESPONSE.copy()


@pytest.fixture
def clean_response():
    return APPLY_GUARDRAIL_CLEAN_RESPONSE.copy()


@pytest.fixture
def denied_topic_response():
    return APPLY_GUARDRAIL_DENIED_TOPIC_RESPONSE.copy()


@pytest.fixture
def content_filter_response():
    return APPLY_GUARDRAIL_CONTENT_FILTER_RESPONSE.copy()


@pytest.fixture
def converse_clean_response():
    return CONVERSE_WITH_GUARDRAIL_RESPONSE.copy()


@pytest.fixture
def converse_intervened_response():
    return CONVERSE_GUARDRAIL_INTERVENED_RESPONSE.copy()
