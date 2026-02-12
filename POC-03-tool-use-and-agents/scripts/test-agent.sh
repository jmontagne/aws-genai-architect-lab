#!/bin/bash
# Test all POC-03 endpoints

BASE_URL="${1:-http://localhost:8080}"

echo "=== POC-03: Tool Use & Bedrock Agents ==="
echo "Base URL: $BASE_URL"
echo ""

echo "--- 1. Health Check ---"
curl -s "$BASE_URL/api/v1/agent/health" | python3 -m json.tool
echo ""

echo "--- 2. Pattern A: Converse API Tool Use ---"
curl -s -X POST "$BASE_URL/api/v1/agent/tool-use" \
  -H "Content-Type: application/json" \
  -d '{
    "message": "Find the cheapest flight from Warsaw to Paris on March 15, 2025",
    "maxIterations": 5,
    "temperature": 0.0
  }' | python3 -m json.tool
echo ""

echo "--- 3. Pattern B: Bedrock Agent ---"
curl -s -X POST "$BASE_URL/api/v1/agent/invoke-agent" \
  -H "Content-Type: application/json" \
  -d '{
    "message": "Find the cheapest flight from Warsaw to Paris on March 15, 2025",
    "sessionId": "test-session-001"
  }' | python3 -m json.tool
echo ""

echo "--- 4. Compare Both Patterns ---"
curl -s -X POST "$BASE_URL/api/v1/agent/compare" \
  -H "Content-Type: application/json" \
  -d '{
    "message": "What flights are available from WAW to CDG on March 15?"
  }' | python3 -m json.tool
echo ""

echo "=== Done ==="
