# --- MASK Guardrail Outputs ---
output "guardrail_mask_id" {
  description = "The ID of the MASK guardrail"
  value       = aws_bedrock_guardrail.mask.guardrail_id
}

output "guardrail_mask_version" {
  description = "The version number of the MASK guardrail"
  value       = aws_bedrock_guardrail_version.mask.version
}

output "guardrail_mask_arn" {
  description = "The ARN of the MASK guardrail"
  value       = aws_bedrock_guardrail.mask.guardrail_arn
}

# --- BLOCK Guardrail Outputs ---
output "guardrail_block_id" {
  description = "The ID of the BLOCK guardrail"
  value       = aws_bedrock_guardrail.block.guardrail_id
}

output "guardrail_block_version" {
  description = "The version number of the BLOCK guardrail"
  value       = aws_bedrock_guardrail_version.block.version
}

output "guardrail_block_arn" {
  description = "The ARN of the BLOCK guardrail"
  value       = aws_bedrock_guardrail.block.guardrail_arn
}
