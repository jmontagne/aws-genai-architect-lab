output "s3_bucket_name" {
  description = "Name of the S3 bucket for Bedrock logs"
  value       = aws_s3_bucket.bedrock_logs.id
}

output "s3_bucket_arn" {
  description = "ARN of the S3 bucket for Bedrock logs"
  value       = aws_s3_bucket.bedrock_logs.arn
}

output "cloudwatch_log_group_name" {
  description = "Name of the CloudWatch log group"
  value       = aws_cloudwatch_log_group.bedrock_logs.name
}

output "cloudwatch_log_group_arn" {
  description = "ARN of the CloudWatch log group"
  value       = aws_cloudwatch_log_group.bedrock_logs.arn
}

output "bedrock_logging_role_arn" {
  description = "ARN of the IAM role used by Bedrock for logging"
  value       = aws_iam_role.bedrock_logging.arn
}

output "kms_key_arn" {
  description = "ARN of the KMS key used for encryption"
  value       = aws_kms_key.bedrock_logging.arn
}

output "kms_key_id" {
  description = "ID of the KMS key used for encryption"
  value       = aws_kms_key.bedrock_logging.key_id
}
