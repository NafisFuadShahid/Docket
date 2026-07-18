#!/usr/bin/env bash
set -euo pipefail

# Block commits that include secrets or dangerous files
STAGED=$(git diff --cached --name-only 2>/dev/null || echo "")
BLOCKED=0

for f in $STAGED; do
  case "$f" in
    .env|*.pem|*.key|*credentials*|*secret*)
      echo "BLOCKED: $f — secrets must not be committed"
      BLOCKED=1
      ;;
  esac
done

if echo "$STAGED" | grep -q "\.env$"; then
  echo "BLOCKED: .env file — use .env.example for templates"
  BLOCKED=1
fi

if git diff --cached -p | grep -qiE '(AKIA[A-Z0-9]{16}|ghp_[a-zA-Z0-9]{36}|sk-[a-zA-Z0-9]{48})'; then
  echo "BLOCKED: Possible API key or secret detected in staged changes"
  BLOCKED=1
fi

exit $BLOCKED
