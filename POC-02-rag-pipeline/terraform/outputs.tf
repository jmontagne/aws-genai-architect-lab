output "knowledge_base_id" {
  description = "The ID of the Bedrock Knowledge Base"
  value       = aws_bedrockagent_knowledge_base.main.id
}

output "knowledge_base_arn" {
  description = "The ARN of the Bedrock Knowledge Base"
  value       = aws_bedrockagent_knowledge_base.main.arn
}

output "data_source_id" {
  description = "The ID of the Knowledge Base data source"
  value       = aws_bedrockagent_data_source.s3.data_source_id
}

output "bucket_name" {
  description = "The name of the S3 bucket for documents"
  value       = aws_s3_bucket.documents.id
}

output "bucket_arn" {
  description = "The ARN of the S3 bucket"
  value       = aws_s3_bucket.documents.arn
}

output "kb_role_arn" {
  description = "The ARN of the Knowledge Base IAM role"
  value       = aws_iam_role.kb_role.arn
}

output "s3_vector_bucket_arn" {
  description = "The ARN of the S3 Vector bucket"
  value       = aws_s3vectors_vector_bucket.main.vector_bucket_arn
}

output "s3_vector_index_arn" {
  description = "The ARN of the vector index"
  value       = aws_s3vectors_index.main.index_arn
}
