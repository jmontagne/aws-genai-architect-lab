# S3 bucket for ground truth datasets and evaluation results.
# Bedrock Model Evaluation requires S3 for input datasets and output reports.

resource "aws_s3_bucket" "evaluation" {
  bucket = "${var.project_name}-${data.aws_caller_identity.current.account_id}-${var.region}"

  force_destroy = true # POC only — allows terraform destroy without emptying bucket
}

resource "aws_s3_bucket_versioning" "evaluation" {
  bucket = aws_s3_bucket.evaluation.id

  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_s3_bucket_server_side_encryption_configuration" "evaluation" {
  bucket = aws_s3_bucket.evaluation.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

resource "aws_s3_bucket_public_access_block" "evaluation" {
  bucket = aws_s3_bucket.evaluation.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}
