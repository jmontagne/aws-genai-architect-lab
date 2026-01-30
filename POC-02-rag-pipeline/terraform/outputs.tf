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

output "opensearch_collection_endpoint" {
  description = "The endpoint of the OpenSearch Serverless collection"
  value       = aws_opensearchserverless_collection.kb.collection_endpoint
}

output "opensearch_collection_arn" {
  description = "The ARN of the OpenSearch Serverless collection"
  value       = aws_opensearchserverless_collection.kb.arn
}
