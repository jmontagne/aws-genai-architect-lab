resource "aws_s3vectors_vector_bucket" "main" {
  vector_bucket_name = "${var.project_name}-vectors"
  force_destroy      = true

  tags = {
    Name = "${var.project_name}-vector-bucket"
  }
}

resource "aws_s3vectors_index" "main" {
  vector_bucket_name = aws_s3vectors_vector_bucket.main.vector_bucket_name
  index_name         = var.s3_vector_index_name
  data_type          = "float32"
  dimension          = 1024
  distance_metric    = "cosine"

  metadata_configuration {
    non_filterable_metadata_keys = ["AMAZON_BEDROCK_TEXT", "AMAZON_BEDROCK_METADATA"]
  }

  tags = {
    Name = "${var.project_name}-vector-index"
  }
}
