variable "aws_region" {
  description = "AWS region for resources"
  type        = string
  default     = "us-east-1"
}

variable "environment" {
  description = "Environment name"
  type        = string
  default     = "dev"
}

variable "project_name" {
  description = "Project name used for resource naming"
  type        = string
  default     = "bedrock-inference-lab"
}

variable "log_retention_days" {
  description = "CloudWatch log retention in days"
  type        = number
  default     = 30
}

variable "enable_s3_logging" {
  description = "Enable S3 logging for Bedrock invocations"
  type        = bool
  default     = true
}

variable "enable_cloudwatch_logging" {
  description = "Enable CloudWatch logging for Bedrock invocations"
  type        = bool
  default     = true
}

variable "log_image_data" {
  description = "Whether to log image data in invocations"
  type        = bool
  default     = false
}

variable "log_embedding_data" {
  description = "Whether to log embedding data in invocations"
  type        = bool
  default     = false
}

# -----------------------------------------------------------------------------
# Lambda Configuration
# -----------------------------------------------------------------------------

variable "lambda_jar_path" {
  description = "Path to the Lambda deployment JAR file"
  type        = string
  default     = "../target/bedrock-inference-lab-1.0.0-SNAPSHOT.jar"
}

variable "lambda_memory_size" {
  description = "Lambda memory size in MB (also affects CPU allocation)"
  type        = number
  default     = 2048  # 2GB for Spring Boot + Bedrock SDK
}

variable "log_level" {
  description = "Log level for Lambda Powertools"
  type        = string
  default     = "INFO"
}
