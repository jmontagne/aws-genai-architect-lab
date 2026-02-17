variable "project_name" {
  description = "Project name used for resource naming"
  type        = string
  default     = "poc04-guardrails"
}

variable "environment" {
  description = "Environment name"
  type        = string
  default     = "dev"
}

variable "region" {
  description = "AWS region"
  type        = string
  default     = "us-east-1"
}
