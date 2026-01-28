# CloudWatch Log Group for Bedrock Logs
resource "aws_cloudwatch_log_group" "bedrock_logs" {
  name              = "/aws/bedrock/${local.resource_prefix}/invocations"
  retention_in_days = var.log_retention_days
  kms_key_id        = aws_kms_key.bedrock_logging.arn

  tags = {
    Name = "${local.resource_prefix}-bedrock-logs"
  }
}
