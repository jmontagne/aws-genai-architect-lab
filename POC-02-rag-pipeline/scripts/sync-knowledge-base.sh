#!/bin/bash
# Sync documents to S3 and trigger Knowledge Base ingestion

set -e

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
TERRAFORM_DIR="$PROJECT_DIR/terraform"
DOCS_DIR="$PROJECT_DIR/test-docs"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}=== Knowledge Base Sync Script ===${NC}"

# Get Terraform outputs
echo -e "${YELLOW}Getting Terraform outputs...${NC}"
cd "$TERRAFORM_DIR"

BUCKET_NAME=$(terraform output -raw bucket_name 2>/dev/null || echo "")
KB_ID=$(terraform output -raw knowledge_base_id 2>/dev/null || echo "")
DS_ID=$(terraform output -raw data_source_id 2>/dev/null || echo "")

if [ -z "$BUCKET_NAME" ] || [ -z "$KB_ID" ] || [ -z "$DS_ID" ]; then
    echo -e "${RED}Error: Could not get Terraform outputs. Make sure you have run 'terraform apply'.${NC}"
    echo "BUCKET_NAME: $BUCKET_NAME"
    echo "KB_ID: $KB_ID"
    echo "DS_ID: $DS_ID"
    exit 1
fi

echo "Bucket: $BUCKET_NAME"
echo "Knowledge Base ID: $KB_ID"
echo "Data Source ID: $DS_ID"

# Sync documents to S3
echo -e "${YELLOW}Syncing documents to S3...${NC}"
aws s3 sync "$DOCS_DIR" "s3://$BUCKET_NAME/" \
    --exclude "*.sh" \
    --exclude ".DS_Store" \
    --exclude "*.gitkeep"

echo -e "${GREEN}Documents synced successfully!${NC}"

# Start ingestion job
echo -e "${YELLOW}Starting Knowledge Base ingestion job...${NC}"
INGESTION_RESPONSE=$(aws bedrock-agent start-ingestion-job \
    --knowledge-base-id "$KB_ID" \
    --data-source-id "$DS_ID" \
    --output json)

JOB_ID=$(echo "$INGESTION_RESPONSE" | jq -r '.ingestionJob.ingestionJobId')
echo "Ingestion Job ID: $JOB_ID"

# Poll for completion
echo -e "${YELLOW}Waiting for ingestion to complete...${NC}"
while true; do
    STATUS_RESPONSE=$(aws bedrock-agent get-ingestion-job \
        --knowledge-base-id "$KB_ID" \
        --data-source-id "$DS_ID" \
        --ingestion-job-id "$JOB_ID" \
        --output json)

    STATUS=$(echo "$STATUS_RESPONSE" | jq -r '.ingestionJob.status')
    echo "Status: $STATUS"

    if [ "$STATUS" = "COMPLETE" ]; then
        echo -e "${GREEN}Ingestion completed successfully!${NC}"
        echo "$STATUS_RESPONSE" | jq '.ingestionJob.statistics'
        break
    elif [ "$STATUS" = "FAILED" ]; then
        echo -e "${RED}Ingestion failed!${NC}"
        echo "$STATUS_RESPONSE" | jq '.ingestionJob.failureReasons'
        exit 1
    fi

    sleep 10
done

echo -e "${GREEN}=== Sync Complete ===${NC}"
