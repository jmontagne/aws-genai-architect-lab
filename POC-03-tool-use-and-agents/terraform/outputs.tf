output "agent_id" {
  description = "The ID of the Bedrock Agent"
  value       = aws_bedrockagent_agent.flight_assistant.id
}

output "agent_alias_id" {
  description = "The ID of the Bedrock Agent alias"
  value       = aws_bedrockagent_agent_alias.live.agent_alias_id
}

output "dynamodb_table_name" {
  description = "The name of the DynamoDB flights table"
  value       = aws_dynamodb_table.flights.name
}

output "lambda_arn" {
  description = "The ARN of the Lambda flight tool handler"
  value       = aws_lambda_function.flight_tool_handler.arn
}
