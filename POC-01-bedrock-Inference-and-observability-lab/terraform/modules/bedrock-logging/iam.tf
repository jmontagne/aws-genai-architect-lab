# IAM Role for Bedrock Logging
resource "aws_iam_role" "bedrock_logging" {
  name = "${local.resource_prefix}-bedrock-logging-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Principal = {
          Service = "bedrock.amazonaws.com"
        }
        Action = "sts:AssumeRole"
        Condition = {
          StringEquals = {
            "aws:SourceAccount" = data.aws_caller_identity.current.account_id
          }
          ArnLike = {
            "aws:SourceArn" = "arn:aws:bedrock:${data.aws_region.current.name}:${data.aws_caller_identity.current.account_id}:*"
          }
        }
      }
    ]
  })

  tags = {
    Name = "${local.resource_prefix}-bedrock-logging-role"
  }
}

# S3 permissions for Bedrock logging
resource "aws_iam_role_policy" "bedrock_logging_s3" {
  name = "${local.resource_prefix}-bedrock-logging-s3"
  role = aws_iam_role.bedrock_logging.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "s3:PutObject",
          "s3:GetBucketLocation"
        ]
        Resource = [
          aws_s3_bucket.bedrock_logs.arn,
          "${aws_s3_bucket.bedrock_logs.arn}/*"
        ]
      },
      {
        Effect = "Allow"
        Action = [
          "kms:Encrypt",
          "kms:GenerateDataKey*",
          "kms:DescribeKey"
        ]
        Resource = aws_kms_key.bedrock_logging.arn
      }
    ]
  })
}

# CloudWatch permissions for Bedrock logging
resource "aws_iam_role_policy" "bedrock_logging_cloudwatch" {
  name = "${local.resource_prefix}-bedrock-logging-cloudwatch"
  role = aws_iam_role.bedrock_logging.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "logs:CreateLogStream",
          "logs:PutLogEvents"
        ]
        Resource = "${aws_cloudwatch_log_group.bedrock_logs.arn}:*"
      }
    ]
  })
}
