variable "project_name" {
  description = "Project name used for resource naming"
  type        = string
  default     = "poc03-agents"
}

variable "environment" {
  description = "Deployment environment"
  type        = string
  default     = "dev"
}

variable "region" {
  description = "AWS region"
  type        = string
  default     = "us-east-1"
}

variable "lambda_jar_path" {
  description = "Path to the Lambda uber-JAR file"
  type        = string
  default     = "../lambda/target/poc03-flight-tool-lambda-1.0.0-SNAPSHOT.jar"
}
