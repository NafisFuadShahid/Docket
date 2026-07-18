---
name: docket-release
description: Run pre-release checks across all three services before deploying
---

# Docket Release Checklist

Run all verification steps before a release.

## Steps

1. **Backend tests**
   ```bash
   cd backend-spring && ./gradlew test
   ```

2. **AI service tests**
   ```bash
   cd ai-service && python -m pytest tests/ -v
   ```

3. **Frontend build**
   ```bash
   cd frontend && npm run build
   ```

4. **Docker Compose build**
   ```bash
   docker compose build
   ```

5. **Docker Compose smoke test**
   ```bash
   docker compose up -d
   sleep 10
   curl -f http://localhost:8080/actuator/health
   curl -f http://localhost:3000
   docker compose down
   ```

6. **Security scan**
   - Check for secrets in committed files: `git log --all -p | grep -iE '(password|secret|api.key).*=.*[a-zA-Z0-9]{8,}'`
   - Verify .env.example has only placeholder values

7. **Report results** — summarize pass/fail for each step
