locals {
  test_docs_dir = "${path.module}/../test-docs"
  test_docs = toset([
    "aws-well-architected.md",
    "aws-well-architected.metadata.json",
    "bedrock-pricing.md",
    "bedrock-pricing.metadata.json",
    "lambda-best-practices.md",
    "lambda-best-practices.metadata.json",
  ])
}

resource "aws_s3_object" "test_docs" {
  for_each = local.test_docs

  bucket       = aws_s3_bucket.documents.id
  key          = each.value
  source       = "${local.test_docs_dir}/${each.value}"
  etag         = filemd5("${local.test_docs_dir}/${each.value}")
  content_type = endswith(each.value, ".json") ? "application/json" : "text/markdown"

  tags = {
    Name = "test-document"
  }
}
