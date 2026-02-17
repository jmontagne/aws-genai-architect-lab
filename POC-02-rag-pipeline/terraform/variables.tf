variable "project_name" {
  description = "Project name used for resource naming"
  type        = string
  default     = "poc02-rag"
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

variable "s3_vector_index_name" {
  description = "Name of the vector index inside the S3 Vector bucket"
  type        = string
  default     = "bedrock-kb-index"
}

variable "chunking_strategy" {
  description = "Chunking strategy: FIXED_SIZE, HIERARCHICAL, SEMANTIC, or NONE"
  type        = string
  default     = "FIXED_SIZE"
}

variable "chunk_max_tokens" {
  description = "Maximum tokens per chunk (for FIXED_SIZE strategy)"
  type        = number
  default     = 300
}

variable "chunk_overlap_percentage" {
  description = "Overlap percentage between chunks (for FIXED_SIZE strategy)"
  type        = number
  default     = 20
}
