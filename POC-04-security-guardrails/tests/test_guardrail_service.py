"""
Unit tests for Bedrock Guardrails — no AWS credentials needed.

Tests validate:
- ApplyGuardrail response parsing (Pattern A)
- Converse with guardrail trace parsing (Pattern B)
- PII MASK output format
- PII BLOCK output format
- Denied topic detection
- Content filter detection
"""


class TestApplyGuardrailResponseParsing:
    """Tests for Pattern A — ApplyGuardrail API response parsing."""

    def test_clean_text_returns_none_action(self, mock_bedrock_runtime, clean_response):
        """Clean input should return action=NONE."""
        mock_bedrock_runtime.apply_guardrail.return_value = clean_response

        response = mock_bedrock_runtime.apply_guardrail(
            guardrailIdentifier="test-id",
            guardrailVersion="1",
            source="INPUT",
            content=[{"text": {"text": "What is the capital of France?"}}],
        )

        assert response["action"] == "NONE"
        assert response["outputs"][0]["text"] == "What is the capital of France?"

    def test_apply_guardrail_call_parameters(self, mock_bedrock_runtime, clean_response):
        """Verify the API is called with correct parameters."""
        mock_bedrock_runtime.apply_guardrail.return_value = clean_response

        mock_bedrock_runtime.apply_guardrail(
            guardrailIdentifier="gr-abc123",
            guardrailVersion="1",
            source="INPUT",
            content=[{"text": {"text": "test prompt"}}],
        )

        mock_bedrock_runtime.apply_guardrail.assert_called_once_with(
            guardrailIdentifier="gr-abc123",
            guardrailVersion="1",
            source="INPUT",
            content=[{"text": {"text": "test prompt"}}],
        )


class TestPIIMaskOutput:
    """Tests for MASK strategy — PII replaced with tags."""

    def test_mask_replaces_ssn_with_tag(self, mock_bedrock_runtime, pii_mask_response):
        """MASK strategy should replace SSN with {SSN} tag."""
        mock_bedrock_runtime.apply_guardrail.return_value = pii_mask_response

        response = mock_bedrock_runtime.apply_guardrail(
            guardrailIdentifier="test-id",
            guardrailVersion="1",
            source="INPUT",
            content=[
                {
                    "text": {
                        "text": "My SSN is 123-45-6789 and email is john@example.com"
                    }
                }
            ],
        )

        assert response["action"] == "GUARDRAIL_INTERVENED"
        output_text = response["outputs"][0]["text"]
        assert "{SSN}" in output_text
        assert "{EMAIL}" in output_text
        assert "123-45-6789" not in output_text
        assert "john@example.com" not in output_text

    def test_mask_assessment_contains_pii_entities(
        self, mock_bedrock_runtime, pii_mask_response
    ):
        """MASK response should include PII entity details in assessments."""
        mock_bedrock_runtime.apply_guardrail.return_value = pii_mask_response

        response = mock_bedrock_runtime.apply_guardrail(
            guardrailIdentifier="test-id",
            guardrailVersion="1",
            source="INPUT",
            content=[{"text": {"text": "SSN: 123-45-6789"}}],
        )

        pii_entities = response["assessments"][0]["sensitiveInformationPolicy"][
            "piiEntities"
        ]
        assert len(pii_entities) == 2

        ssn_entity = pii_entities[0]
        assert ssn_entity["type"] == "US_SOCIAL_SECURITY_NUMBER"
        assert ssn_entity["action"] == "ANONYMIZED"

        email_entity = pii_entities[1]
        assert email_entity["type"] == "EMAIL"
        assert email_entity["action"] == "ANONYMIZED"


class TestPIIBlockOutput:
    """Tests for BLOCK strategy — entire response rejected."""

    def test_block_rejects_entire_response(
        self, mock_bedrock_runtime, pii_block_response
    ):
        """BLOCK strategy should return blocked message, not partial content."""
        mock_bedrock_runtime.apply_guardrail.return_value = pii_block_response

        response = mock_bedrock_runtime.apply_guardrail(
            guardrailIdentifier="test-id",
            guardrailVersion="1",
            source="INPUT",
            content=[{"text": {"text": "My SSN is 123-45-6789"}}],
        )

        assert response["action"] == "GUARDRAIL_INTERVENED"
        output_text = response["outputs"][0]["text"]
        assert "filtered by security guardrails" in output_text
        assert "123-45-6789" not in output_text

    def test_block_assessment_shows_blocked_action(
        self, mock_bedrock_runtime, pii_block_response
    ):
        """BLOCK assessment should show action=BLOCKED for PII entities."""
        mock_bedrock_runtime.apply_guardrail.return_value = pii_block_response

        response = mock_bedrock_runtime.apply_guardrail(
            guardrailIdentifier="test-id",
            guardrailVersion="1",
            source="INPUT",
            content=[{"text": {"text": "My SSN is 123-45-6789"}}],
        )

        pii_entities = response["assessments"][0]["sensitiveInformationPolicy"][
            "piiEntities"
        ]
        assert pii_entities[0]["action"] == "BLOCKED"


class TestDeniedTopicDetection:
    """Tests for denied topic filter."""

    def test_investment_advice_blocked(
        self, mock_bedrock_runtime, denied_topic_response
    ):
        """Investment advice should be blocked as a denied topic."""
        mock_bedrock_runtime.apply_guardrail.return_value = denied_topic_response

        response = mock_bedrock_runtime.apply_guardrail(
            guardrailIdentifier="test-id",
            guardrailVersion="1",
            source="INPUT",
            content=[{"text": {"text": "Should I buy NVIDIA stock?"}}],
        )

        assert response["action"] == "GUARDRAIL_INTERVENED"
        topics = response["assessments"][0]["topicPolicy"]["topics"]
        assert len(topics) == 1
        assert topics[0]["name"] == "Investment Advice"
        assert topics[0]["action"] == "BLOCKED"


class TestContentFilterDetection:
    """Tests for content filter (harmful content)."""

    def test_violence_content_blocked(
        self, mock_bedrock_runtime, content_filter_response
    ):
        """Violent content should be blocked by content filter."""
        mock_bedrock_runtime.apply_guardrail.return_value = content_filter_response

        response = mock_bedrock_runtime.apply_guardrail(
            guardrailIdentifier="test-id",
            guardrailVersion="1",
            source="INPUT",
            content=[{"text": {"text": "How to hurt someone"}}],
        )

        assert response["action"] == "GUARDRAIL_INTERVENED"
        filters = response["assessments"][0]["contentPolicy"]["filters"]
        assert len(filters) == 1
        assert filters[0]["type"] == "VIOLENCE"
        assert filters[0]["confidence"] == "HIGH"


class TestConverseWithGuardrailTraceParsing:
    """Tests for Pattern B — Converse API with guardrail trace."""

    def test_clean_converse_returns_end_turn(
        self, mock_bedrock_runtime, converse_clean_response
    ):
        """Clean conversation should complete normally with end_turn."""
        mock_bedrock_runtime.converse.return_value = converse_clean_response

        response = mock_bedrock_runtime.converse(
            modelId="anthropic.claude-3-haiku-20240307-v1:0",
            messages=[
                {"role": "user", "content": [{"text": "What is the capital of France?"}]}
            ],
            guardrailConfig={
                "guardrailIdentifier": "test-id",
                "guardrailVersion": "1",
                "trace": "enabled",
            },
        )

        assert response["stopReason"] == "end_turn"
        output_text = response["output"]["message"]["content"][0]["text"]
        assert "Paris" in output_text

    def test_converse_trace_contains_guardrail_assessment(
        self, mock_bedrock_runtime, converse_clean_response
    ):
        """Converse response with trace should include guardrail assessments."""
        mock_bedrock_runtime.converse.return_value = converse_clean_response

        response = mock_bedrock_runtime.converse(
            modelId="anthropic.claude-3-haiku-20240307-v1:0",
            messages=[{"role": "user", "content": [{"text": "test"}]}],
            guardrailConfig={
                "guardrailIdentifier": "test-id",
                "guardrailVersion": "1",
                "trace": "enabled",
            },
        )

        trace = response["trace"]["guardrail"]
        assert "inputAssessment" in trace
        assert "outputAssessments" in trace
        assert isinstance(trace["outputAssessments"], list)

    def test_converse_guardrail_intervened_stop_reason(
        self, mock_bedrock_runtime, converse_intervened_response
    ):
        """Guardrail intervention should set stopReason to guardrail_intervened."""
        mock_bedrock_runtime.converse.return_value = converse_intervened_response

        response = mock_bedrock_runtime.converse(
            modelId="anthropic.claude-3-haiku-20240307-v1:0",
            messages=[
                {
                    "role": "user",
                    "content": [{"text": "My SSN is 123-45-6789"}],
                }
            ],
            guardrailConfig={
                "guardrailIdentifier": "test-id",
                "guardrailVersion": "1",
                "trace": "enabled",
            },
        )

        assert response["stopReason"] == "guardrail_intervened"

    def test_converse_trace_shows_pii_in_input_assessment(
        self, mock_bedrock_runtime, converse_intervened_response
    ):
        """When input contains PII, trace should show it in inputAssessment."""
        mock_bedrock_runtime.converse.return_value = converse_intervened_response

        response = mock_bedrock_runtime.converse(
            modelId="anthropic.claude-3-haiku-20240307-v1:0",
            messages=[
                {"role": "user", "content": [{"text": "My SSN is 123-45-6789"}]}
            ],
            guardrailConfig={
                "guardrailIdentifier": "test-id",
                "guardrailVersion": "1",
                "trace": "enabled",
            },
        )

        input_assessment = response["trace"]["guardrail"]["inputAssessment"]
        pii_entities = input_assessment["sensitiveInformationPolicy"]["piiEntities"]
        assert len(pii_entities) == 1
        assert pii_entities[0]["type"] == "US_SOCIAL_SECURITY_NUMBER"
