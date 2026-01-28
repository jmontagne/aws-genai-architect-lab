variable "project_name" {
  description = "Project name used for resource naming"
  type        = string
}

variable "environment" {
  description = "Environment name"
  type        = string
}

variable "aws_region" {
  description = "AWS region"
  type        = string
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
