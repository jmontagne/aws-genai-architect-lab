"""
MASK vs BLOCK -- Side-by-Side Comparison

Sends the same PII-containing prompts to both the MASK guardrail and the
BLOCK guardrail, displaying results side by side to illustrate the difference.

Usage:
    python scripts/compare_mask_vs_block.py

Requires:
    - Both guardrails deployed via Terraform
    - GUARDRAIL_MASK_ID and GUARDRAIL_BLOCK_ID set (from terraform output)
"""

import json
import os

import boto3

REGION = os.environ.get("AWS_REGION", "us-east-1")
GUARDRAIL_MASK_ID = os.environ.get("GUARDRAIL_MASK_ID", "<FROM_TERRAFORM_OUTPUT>")
GUARDRAIL_BLOCK_ID = os.environ.get("GUARDRAIL_BLOCK_ID", "<FROM_TERRAFORM_OUTPUT>")
GUARDRAIL_VERSION = os.environ.get("GUARDRAIL_VERSION", "1")


def apply_guardrail(client, guardrail_id: str, text: str, source: str = "OUTPUT") -> dict:
    """Call ApplyGuardrail API with a specific guardrail.

    Uses source=OUTPUT because ANONYMIZE (MASK) only triggers on output evaluation.
    BLOCK triggers on both INPUT and OUTPUT.
    """
    return client.apply_guardrail(
        guardrailIdentifier=guardrail_id,
        guardrailVersion=GUARDRAIL_VERSION,
        source=source,
        content=[{"text": {"text": text}}],
    )


def extract_output(response: dict) -> str:
    """Extract the output text from guardrail response."""
    outputs = response.get("outputs", [])
    return outputs[0].get("text", "(no output)") if outputs else "(no output)"


def main():
    client = boto3.client("bedrock-runtime", region_name=REGION)

    pii_prompts = [
        {
            "label": "SSN",
            "text": "My social security number is 123-45-6789.",
        },
        {
            "label": "Email + Phone",
            "text": "Contact me at jane.doe@example.com or call +1-555-123-4567.",
        },
        {
            "label": "Credit Card",
            "text": "Charge $99.99 to my card 4111-1111-1111-1111, exp 03/27.",
        },
        {
            "label": "Multiple PII types",
            "text": "Patient John Smith, SSN 999-88-7777, email john@hospital.org, phone 555-0199, card 5500-0000-0000-0004.",
        },
        {
            "label": "Polish PESEL (regex)",
            "text": "Employee PESEL: 92071012345, please update payroll records.",
        },
    ]

    print("=" * 70)
    print("POC-04: MASK vs BLOCK - Side-by-Side Comparison")
    print("=" * 70)
    print(f"MASK Guardrail ID:  {GUARDRAIL_MASK_ID}")
    print(f"BLOCK Guardrail ID: {GUARDRAIL_BLOCK_ID}")
    print(f"Version: {GUARDRAIL_VERSION}")
    print("=" * 70)

    for prompt in pii_prompts:
        print(f"\n{'-'*70}")
        print(f"TEST: {prompt['label']}")
        print(f"INPUT: {prompt['text']}")
        print(f"{'-'*70}")

        # MASK result
        try:
            mask_response = apply_guardrail(client, GUARDRAIL_MASK_ID, prompt["text"])
            mask_action = mask_response.get("action", "UNKNOWN")
            mask_output = extract_output(mask_response)
        except Exception as e:
            mask_action = "ERROR"
            mask_output = str(e)

        # BLOCK result
        try:
            block_response = apply_guardrail(client, GUARDRAIL_BLOCK_ID, prompt["text"])
            block_action = block_response.get("action", "UNKNOWN")
            block_output = extract_output(block_response)
        except Exception as e:
            block_action = "ERROR"
            block_output = str(e)

        print(f"\n  MASK Strategy:")
        print(f"    Action: {mask_action}")
        print(f"    Output: {mask_output}")
        print(f"\n  BLOCK Strategy:")
        print(f"    Action: {block_action}")
        print(f"    Output: {block_output}")

    print(f"\n{'='*70}")
    print("KEY TAKEAWAY:")
    print("  MASK  -> PII replaced with tags ({SSN}, {EMAIL}, etc.), response preserved")
    print("  BLOCK -> Entire response rejected, no partial data leaks")
    print(f"{'='*70}")


if __name__ == "__main__":
    main()
