resource "aws_bedrockagent_agent" "flight_assistant" {
  agent_name                  = "${var.project_name}-flight-assistant"
  agent_resource_role_arn     = aws_iam_role.agent_role.arn
  foundation_model            = "anthropic.claude-3-5-sonnet-20241022-v2:0"
  idle_session_ttl_in_seconds = 600

  instruction = <<-EOT
    You are a flight booking assistant. You help users search for flights
    and get flight details. Always use the available tools to look up
    real data. Never make up flight information. When presenting results,
    include the flight ID, airline, departure time, arrival time, and price.
  EOT

  tags = {
    Name = "${var.project_name}-flight-assistant"
  }
}

resource "aws_bedrockagent_agent_action_group" "flight_tools" {
  agent_id                   = aws_bedrockagent_agent.flight_assistant.id
  agent_version              = "DRAFT"
  action_group_name          = "FlightTools"
  skip_resource_in_use_check = true

  action_group_executor {
    lambda = aws_lambda_function.flight_tool_handler.arn
  }

  api_schema {
    payload = file("${path.module}/../openapi/flight-tools.yaml")
  }
}

resource "null_resource" "prepare_agent" {
  triggers = {
    agent_id     = aws_bedrockagent_agent.flight_assistant.id
    action_group = aws_bedrockagent_agent_action_group.flight_tools.action_group_name
  }

  provisioner "local-exec" {
    command = "aws bedrock-agent prepare-agent --agent-id ${aws_bedrockagent_agent.flight_assistant.id} --region ${var.region}"
  }

  depends_on = [
    aws_bedrockagent_agent_action_group.flight_tools
  ]
}

resource "aws_bedrockagent_agent_alias" "live" {
  agent_id         = aws_bedrockagent_agent.flight_assistant.id
  agent_alias_name = "live"

  depends_on = [
    null_resource.prepare_agent
  ]
}
