resource "aws_lambda_function" "flight_tool_handler" {
  function_name = "${var.project_name}-flight-tool-handler"
  role          = aws_iam_role.lambda_role.arn
  handler       = "com.awslab.agent.lambda.FlightToolHandler::handleRequest"
  runtime       = "java21"
  timeout       = 30
  memory_size   = 512

  filename         = var.lambda_jar_path
  source_code_hash = filebase64sha256(var.lambda_jar_path)

  environment {
    variables = {
      TABLE_NAME = aws_dynamodb_table.flights.name
    }
  }

  tags = {
    Name = "${var.project_name}-flight-tool-handler"
  }

  depends_on = [
    aws_iam_role_policy_attachment.lambda_basic,
    aws_iam_role_policy.lambda_dynamodb
  ]
}

resource "aws_lambda_permission" "bedrock_invoke" {
  statement_id  = "AllowBedrockInvoke"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.flight_tool_handler.function_name
  principal     = "bedrock.amazonaws.com"
  source_arn    = aws_bedrockagent_agent.flight_assistant.agent_arn
}
