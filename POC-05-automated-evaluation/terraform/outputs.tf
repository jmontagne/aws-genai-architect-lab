# --- S3 Bucket Outputs ---
output "evaluation_bucket_name" {
  description = "S3 bucket name for evaluation datasets and results"
  value       = aws_s3_bucket.evaluation.id
}

output "evaluation_bucket_arn" {
  description = "S3 bucket ARN"
  value       = aws_s3_bucket.evaluation.arn
}

# --- IAM Role Outputs ---
output "evaluation_role_arn" {
  description = "IAM role ARN for Bedrock evaluation jobs"
  value       = aws_iam_role.bedrock_evaluation.arn
}

output "evaluation_role_name" {
  description = "IAM role name"
  value       = aws_iam_role.bedrock_evaluation.name
}

# --- Account Info ---
output "account_id" {
  description = "AWS account ID"
  value       = data.aws_caller_identity.current.account_id
}

output "region" {
  description = "AWS region"
  value       = data.aws_region.current.name
}
