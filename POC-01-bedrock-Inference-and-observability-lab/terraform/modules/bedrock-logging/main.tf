data "aws_caller_identity" "current" {}
data "aws_region" "current" {}

locals {
  resource_prefix = "${var.project_name}-${var.environment}"
}

# KMS Key for encryption
resource "aws_kms_key" "bedrock_logging" {
  description             = "KMS key for Bedrock logging encryption"
  deletion_window_in_days = 7
  enable_key_rotation     = true

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid    = "Enable IAM User Permissions"
        Effect = "Allow"
        Principal = {
          AWS = "arn:aws:iam::${data.aws_caller_identity.current.account_id}:root"
        }
        Action   = "kms:*"
        Resource = "*"
      },
      {
        Sid    = "Allow Bedrock Service"
        Effect = "Allow"
        Principal = {
          Service = "bedrock.amazonaws.com"
        }
        Action = [
          "kms:Encrypt",
          "kms:Decrypt",
          "kms:GenerateDataKey*",
          "kms:DescribeKey"
        ]
        Resource = "*"
        Condition = {
          StringEquals = {
            "aws:SourceAccount" = data.aws_caller_identity.current.account_id
          }
        }
      },
      {
        Sid    = "Allow CloudWatch Logs"
        Effect = "Allow"
        Principal = {
          Service = "logs.${data.aws_region.current.name}.amazonaws.com"
        }
        Action = [
          "kms:Encrypt",
          "kms:Decrypt",
          "kms:GenerateDataKey*",
          "kms:DescribeKey"
        ]
        Resource = "*"
        Condition = {
          ArnLike = {
            "kms:EncryptionContext:aws:logs:arn" = "arn:aws:logs:${data.aws_region.current.name}:${data.aws_caller_identity.current.account_id}:log-group:*"
          }
        }
      }
    ]
  })

  tags = {
    Name = "${local.resource_prefix}-bedrock-logging-key"
  }
}

resource "aws_kms_alias" "bedrock_logging" {
  name          = "alias/${local.resource_prefix}-bedrock-logging"
  target_key_id = aws_kms_key.bedrock_logging.key_id
}

# Bedrock Model Invocation Logging Configuration
resource "aws_bedrock_model_invocation_logging_configuration" "this" {
  logging_config {
    embedding_data_delivery_enabled = var.log_embedding_data
    image_data_delivery_enabled     = var.log_image_data
    text_data_delivery_enabled      = true

    dynamic "cloudwatch_config" {
      for_each = var.enable_cloudwatch_logging ? [1] : []
      content {
        log_group_name = aws_cloudwatch_log_group.bedrock_logs.name
        role_arn       = aws_iam_role.bedrock_logging.arn

        large_data_delivery_s3_config {
          bucket_name = aws_s3_bucket.bedrock_logs.id
          key_prefix  = "large-data/"
        }
      }
    }

    dynamic "s3_config" {
      for_each = var.enable_s3_logging ? [1] : []
      content {
        bucket_name = aws_s3_bucket.bedrock_logs.id
        key_prefix  = "invocation-logs/"
      }
    }
  }

  depends_on = [
    aws_iam_role_policy.bedrock_logging_s3,
    aws_iam_role_policy.bedrock_logging_cloudwatch,
    aws_s3_bucket_policy.bedrock_logs
  ]
}
