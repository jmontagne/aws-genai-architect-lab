# =============================================================================
# AWS Lambda Function with SnapStart for Spring Boot Bedrock Inference
# =============================================================================

# -----------------------------------------------------------------------------
# IAM Role for Lambda
# -----------------------------------------------------------------------------
resource "aws_iam_role" "lambda_role" {
  name = "${var.project_name}-${var.environment}-lambda-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "lambda.amazonaws.com"
        }
      }
    ]
  })

  tags = local.common_tags
}

# Lambda basic execution (CloudWatch Logs)
resource "aws_iam_role_policy_attachment" "lambda_basic" {
  role       = aws_iam_role.lambda_role.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole"
}

# X-Ray tracing for Powertools
resource "aws_iam_role_policy_attachment" "lambda_xray" {
  role       = aws_iam_role.lambda_role.name
  policy_arn = "arn:aws:iam::aws:policy/AWSXRayDaemonWriteAccess"
}

# Bedrock access policy
resource "aws_iam_role_policy" "lambda_bedrock" {
  name = "${var.project_name}-${var.environment}-bedrock-policy"
  role = aws_iam_role.lambda_role.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid    = "BedrockInvoke"
        Effect = "Allow"
        Action = [
          "bedrock:InvokeModel",
          "bedrock:InvokeModelWithResponseStream"
        ]
        Resource = [
          "arn:aws:bedrock:${var.aws_region}::foundation-model/anthropic.claude-3-5-sonnet-20240620-v1:0",
          "arn:aws:bedrock:${var.aws_region}::foundation-model/anthropic.claude-3-haiku-20240307-v1:0"
        ]
      }
    ]
  })
}

# -----------------------------------------------------------------------------
# Lambda Function
# -----------------------------------------------------------------------------
resource "aws_lambda_function" "bedrock_inference" {
  function_name = "${var.project_name}-${var.environment}"
  role          = aws_iam_role.lambda_role.arn
  handler       = "com.jmontagne.bedrock.StreamLambdaHandler::handleRequest"
  runtime       = "java21"
  timeout       = 120  # 2 minutes for LLM responses
  memory_size   = var.lambda_memory_size

  # JAR file location (built by Maven)
  filename         = var.lambda_jar_path
  source_code_hash = filebase64sha256(var.lambda_jar_path)

  # SnapStart for fast cold starts (~200ms instead of ~5s)
  snap_start {
    apply_on = "PublishedVersions"
  }

  # Environment variables for Powertools
  environment {
    variables = {
      # AWS Lambda Powertools configuration
      POWERTOOLS_SERVICE_NAME = var.project_name
      POWERTOOLS_LOG_LEVEL    = var.log_level
      POWERTOOLS_METRICS_NAMESPACE = "${var.project_name}-${var.environment}"

      # Spring Boot configuration
      SPRING_PROFILES_ACTIVE = "lambda"

      # Java options for Lambda (AWS_REGION is already set by Lambda runtime)
      JAVA_TOOL_OPTIONS = "-XX:+TieredCompilation -XX:TieredStopAtLevel=1"
    }
  }

  # X-Ray tracing
  tracing_config {
    mode = "Active"
  }

  tags = local.common_tags

  depends_on = [
    aws_iam_role_policy_attachment.lambda_basic,
    aws_iam_role_policy_attachment.lambda_xray,
    aws_iam_role_policy.lambda_bedrock
  ]
}

# Publish a version (required for SnapStart)
resource "aws_lambda_alias" "live" {
  name             = "live"
  function_name    = aws_lambda_function.bedrock_inference.function_name
  function_version = aws_lambda_function.bedrock_inference.version

  depends_on = [aws_lambda_function.bedrock_inference]
}

# -----------------------------------------------------------------------------
# Lambda Function URL (for direct HTTP access with streaming support)
# -----------------------------------------------------------------------------
resource "aws_lambda_function_url" "bedrock_inference" {
  function_name      = aws_lambda_function.bedrock_inference.function_name
  qualifier          = aws_lambda_alias.live.name
  authorization_type = "NONE"  # Public access for POC; use IAM in production

  cors {
    allow_origins     = ["*"]
    allow_methods     = ["*"]
    allow_headers     = ["*"]
    expose_headers    = ["*"]
    max_age           = 3600
    allow_credentials = false
  }
}

# -----------------------------------------------------------------------------
# API Gateway HTTP API (alternative to Function URL)
# -----------------------------------------------------------------------------
resource "aws_apigatewayv2_api" "bedrock_api" {
  name          = "${var.project_name}-${var.environment}-api"
  protocol_type = "HTTP"
  description   = "HTTP API for Bedrock Inference Lab"

  cors_configuration {
    allow_origins     = ["*"]
    allow_methods     = ["GET", "POST", "OPTIONS"]
    allow_headers     = ["*"]
    expose_headers    = ["*"]
    max_age           = 3600
    allow_credentials = false
  }

  tags = local.common_tags
}

resource "aws_apigatewayv2_stage" "default" {
  api_id      = aws_apigatewayv2_api.bedrock_api.id
  name        = "$default"
  auto_deploy = true

  access_log_settings {
    destination_arn = aws_cloudwatch_log_group.api_gateway.arn
    format = jsonencode({
      requestId      = "$context.requestId"
      ip             = "$context.identity.sourceIp"
      requestTime    = "$context.requestTime"
      httpMethod     = "$context.httpMethod"
      routeKey       = "$context.routeKey"
      status         = "$context.status"
      responseLength = "$context.responseLength"
      integrationLatency = "$context.integrationLatency"
    })
  }

  tags = local.common_tags
}

resource "aws_cloudwatch_log_group" "api_gateway" {
  name              = "/aws/apigateway/${var.project_name}-${var.environment}"
  retention_in_days = var.log_retention_days

  tags = local.common_tags
}

resource "aws_apigatewayv2_integration" "lambda" {
  api_id                 = aws_apigatewayv2_api.bedrock_api.id
  integration_type       = "AWS_PROXY"
  integration_uri        = aws_lambda_alias.live.invoke_arn
  payload_format_version = "2.0"
}

resource "aws_apigatewayv2_route" "default" {
  api_id    = aws_apigatewayv2_api.bedrock_api.id
  route_key = "$default"
  target    = "integrations/${aws_apigatewayv2_integration.lambda.id}"
}

# Allow API Gateway to invoke Lambda
resource "aws_lambda_permission" "api_gateway" {
  statement_id  = "AllowAPIGatewayInvoke"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.bedrock_inference.function_name
  qualifier     = aws_lambda_alias.live.name
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_apigatewayv2_api.bedrock_api.execution_arn}/*/*"
}

# -----------------------------------------------------------------------------
# CloudWatch Log Group for Lambda
# -----------------------------------------------------------------------------
resource "aws_cloudwatch_log_group" "lambda" {
  name              = "/aws/lambda/${aws_lambda_function.bedrock_inference.function_name}"
  retention_in_days = var.log_retention_days

  tags = local.common_tags
}

# -----------------------------------------------------------------------------
# Common Tags
# -----------------------------------------------------------------------------
locals {
  common_tags = {
    Project     = var.project_name
    Environment = var.environment
    ManagedBy   = "terraform"
    Purpose     = "bedrock-inference-poc"
  }
}
