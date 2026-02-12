resource "aws_dynamodb_table" "flights" {
  name         = "${var.project_name}-flights"
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "route"
  range_key    = "flightId"

  attribute {
    name = "route"
    type = "S"
  }

  attribute {
    name = "flightId"
    type = "S"
  }

  attribute {
    name = "date"
    type = "S"
  }

  global_secondary_index {
    name            = "date-index"
    hash_key        = "route"
    range_key       = "date"
    projection_type = "ALL"
  }

  tags = {
    Name = "${var.project_name}-flights"
  }
}
