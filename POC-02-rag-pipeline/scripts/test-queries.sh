#!/bin/bash
# Test queries against the RAG pipeline

set -e

# Configuration
BASE_URL="${BASE_URL:-http://localhost:8080}"
API_PATH="/api/v1"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${GREEN}=== RAG Pipeline Test Queries ===${NC}"
echo "Base URL: $BASE_URL"
echo ""

# Function to make API call and display result
test_endpoint() {
    local endpoint=$1
    local data=$2
    local description=$3

    echo -e "${BLUE}--- $description ---${NC}"
    echo "Endpoint: POST $endpoint"
    echo "Request: $data"
    echo ""

    response=$(curl -s -X POST "${BASE_URL}${API_PATH}${endpoint}" \
        -H "Content-Type: application/json" \
        -d "$data")

    echo "Response:"
    echo "$response" | jq '.' 2>/dev/null || echo "$response"
    echo ""
    echo ""
}

# Health check
echo -e "${YELLOW}Checking service health...${NC}"
health_response=$(curl -s "${BASE_URL}${API_PATH}/health")
echo "$health_response" | jq '.' 2>/dev/null || echo "$health_response"
echo ""

# Test 1: Basic Retrieval
test_endpoint "/retrieve" \
    '{"query": "What are the pillars of AWS Well-Architected Framework?", "numberOfResults": 5}' \
    "Test 1: Basic Retrieval - Well-Architected Pillars"

# Test 2: RAG Generation
test_endpoint "/generate" \
    '{"query": "How should I optimize Lambda cold starts?", "numberOfResults": 5, "temperature": 0.0}' \
    "Test 2: RAG Generation - Lambda Cold Starts"

# Test 3: Filtered Retrieval (by category)
test_endpoint "/retrieve" \
    '{"query": "What are the security best practices?", "numberOfResults": 5, "filter": {"category": "architecture"}}' \
    "Test 3: Filtered Retrieval - Security in Architecture"

# Test 4: Pricing Query
test_endpoint "/generate" \
    '{"query": "How much does Claude 3 Sonnet cost on Amazon Bedrock?", "numberOfResults": 3}' \
    "Test 4: RAG Generation - Bedrock Pricing"

# Test 5: Evaluation
test_endpoint "/evaluate" \
    '{
        "query": "What is operational excellence?",
        "answer": "Operational excellence focuses on running and monitoring systems to deliver business value.",
        "retrievedChunks": [
            "The operational excellence pillar focuses on running and monitoring systems to deliver business value, and continually improving processes and procedures.",
            "Key topics include automating changes, responding to events, and defining standards to manage daily operations."
        ]
    }' \
    "Test 5: Evaluation - Relevance and Groundedness"

# Test 6: Hybrid Search
test_endpoint "/retrieve" \
    '{"query": "provisioned concurrency Lambda", "numberOfResults": 5, "searchType": "HYBRID"}' \
    "Test 6: Hybrid Search - Provisioned Concurrency"

# Test 7: Semantic Search
test_endpoint "/retrieve" \
    '{"query": "how to make serverless functions faster", "numberOfResults": 5, "searchType": "SEMANTIC"}' \
    "Test 7: Semantic Search - Performance"

echo -e "${GREEN}=== All Tests Complete ===${NC}"
