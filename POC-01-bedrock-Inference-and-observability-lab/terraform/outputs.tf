output "s3_bucket_name" {
  description = "Name of the S3 bucket for Bedrock logs"
  value       = module.bedrock_logging.s3_bucket_name
}

output "s3_bucket_arn" {
  description = "ARN of the S3 bucket for Bedrock logs"
  value       = module.bedrock_logging.s3_bucket_arn
}

output "cloudwatch_log_group_name" {
  description = "Name of the CloudWatch log group"
  value       = module.bedrock_logging.cloudwatch_log_group_name
}

output "cloudwatch_log_group_arn" {
  description = "ARN of the CloudWatch log group"
  value       = module.bedrock_logging.cloudwatch_log_group_arn
}

output "bedrock_logging_role_arn" {
  description = "ARN of the IAM role used by Bedrock for logging"
  value       = module.bedrock_logging.bedrock_logging_role_arn
}

output "kms_key_arn" {
  description = "ARN of the KMS key used for encryption"
  value       = module.bedrock_logging.kms_key_arn
}

# -----------------------------------------------------------------------------
# Lambda Outputs
# -----------------------------------------------------------------------------

output "lambda_function_name" {
  description = "Name of the Lambda function"
  value       = aws_lambda_function.bedrock_inference.function_name
}

output "lambda_function_arn" {
  description = "ARN of the Lambda function"
  value       = aws_lambda_function.bedrock_inference.arn
}

output "lambda_function_url" {
  description = "Lambda Function URL (direct HTTP access)"
  value       = aws_lambda_function_url.bedrock_inference.function_url
}

output "api_gateway_url" {
  description = "API Gateway HTTP API URL"
  value       = aws_apigatewayv2_api.bedrock_api.api_endpoint
}

output "api_health_endpoint" {
  description = "Health check endpoint"
  value       = "${aws_apigatewayv2_api.bedrock_api.api_endpoint}/api/v1/inference/health"
}

output "api_models_endpoint" {
  description = "List models endpoint"
  value       = "${aws_apigatewayv2_api.bedrock_api.api_endpoint}/api/v1/inference/models"
}

output "api_stream_endpoint_example" {
  description = "Example streaming inference endpoint"
  value       = "${aws_apigatewayv2_api.bedrock_api.api_endpoint}/api/v1/inference/stream/CLAUDE_3_HAIKU?message=Hello"
}
