#!/usr/bin/env bash
set -euo pipefail

# Run relevant test suites based on which files changed
CHANGED=$(git diff --cached --name-only 2>/dev/null || echo "")

EXIT_CODE=0

if echo "$CHANGED" | grep -q "^backend-spring/"; then
  echo "Running Spring Boot tests..."
  (cd backend-spring && ./gradlew test --quiet) || EXIT_CODE=1
fi

if echo "$CHANGED" | grep -q "^ai-service/"; then
  echo "Running AI service tests..."
  (cd ai-service && python -m pytest tests/ -q) || EXIT_CODE=1
fi

if echo "$CHANGED" | grep -q "^frontend/"; then
  echo "Checking frontend build..."
  (cd frontend && npx next build) || EXIT_CODE=1
fi

exit $EXIT_CODE
