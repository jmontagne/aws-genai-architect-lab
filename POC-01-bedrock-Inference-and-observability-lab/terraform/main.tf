data "aws_caller_identity" "current" {}
data "aws_region" "current" {}

module "bedrock_logging" {
  source = "./modules/bedrock-logging"

  project_name              = var.project_name
  environment               = var.environment
  aws_region                = var.aws_region
  log_retention_days        = var.log_retention_days
  enable_s3_logging         = var.enable_s3_logging
  enable_cloudwatch_logging = var.enable_cloudwatch_logging
  log_image_data            = var.log_image_data
  log_embedding_data        = var.log_embedding_data
}
