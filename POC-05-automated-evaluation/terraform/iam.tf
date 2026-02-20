# IAM role for Bedrock Model Evaluation jobs.
# Bedrock assumes this role to read datasets from S3 and write results.

resource "aws_iam_role" "bedrock_evaluation" {
  name = "${var.project_name}-bedrock-eval-role"

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
        }
      }
    ]
  })
}

resource "aws_iam_role_policy" "bedrock_evaluation_s3" {
  name = "${var.project_name}-s3-access"
  role = aws_iam_role.bedrock_evaluation.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "s3:GetObject",
          "s3:PutObject",
          "s3:ListBucket"
        ]
        Resource = [
          aws_s3_bucket.evaluation.arn,
          "${aws_s3_bucket.evaluation.arn}/*"
        ]
      }
    ]
  })
}

resource "aws_iam_role_policy" "bedrock_evaluation_invoke" {
  name = "${var.project_name}-bedrock-invoke"
  role = aws_iam_role.bedrock_evaluation.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "bedrock:InvokeModel",
          "bedrock:CreateModelEvaluationJob",
          "bedrock:GetModelEvaluationJob",
          "bedrock:ListModelEvaluationJobs"
        ]
        Resource = "*"
      }
    ]
  })
}
