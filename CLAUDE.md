# Docket — Bangladesh Banking Compliance Platform

## Architecture

Hybrid enterprise architecture with three services:

- **backend-spring/** — Spring Boot 3.4.1 (Java 21). Core platform: auth, RBAC, CRUD, workflows, audit logging. PostgreSQL via JPA/Hibernate, Flyway migrations.
- **ai-service/** — Python FastAPI. Document intelligence: regulatory crawling, PDF extraction, LLM-based obligation extraction, compliance Q&A assistant.
- **frontend/** — Next.js 16 App Router + shadcn/ui v4 (Base UI) + Tailwind CSS. Enterprise dashboard with 14 pages.

Services communicate via HTTP: Spring Boot calls AI service for crawl/extraction triggers, AI service posts results back via callback endpoints.

## Tech Stack

| Layer | Tech |
|-------|------|
| Backend | Spring Boot 3.4.1, Java 21, Spring Security, JPA/Hibernate, Flyway, jjwt 0.12.6 |
| AI Service | Python 3.12, FastAPI, pdfplumber, OpenAI SDK, BeautifulSoup, structlog |
| Frontend | Next.js 16, React 19, shadcn/ui v4, Tailwind CSS v4, TypeScript |
| Database | PostgreSQL 16 (H2 in PostgreSQL mode for tests) |
| Infra | Docker Compose, Gradle 8.12 (wrapper included) |

## Test Commands

```bash
# Spring Boot (from backend-spring/)
./gradlew test

# AI service (from ai-service/)
source .venv/bin/activate && python -m pytest tests/ -v

# Frontend (from frontend/)
npx tsc --noEmit
npx next build
```

## Running Locally

```bash
docker compose up        # starts postgres, backend, ai-service, frontend
```

Or individually:
- Backend: `cd backend-spring && ./gradlew bootRun`
- AI service: `cd ai-service && source .venv/bin/activate && uvicorn app.main:app --port 8000`
- Frontend: `cd frontend && npm run dev`

## Project Rules

### Security
- Never commit secrets, API keys, or credentials. Use environment variables.
- JWT secret must be ≥32 characters in production.
- All tenant data access must go through TenantContext (set by JwtAuthFilter).
- Crawler only hits allowlisted domains: bb.org.bd, bfiu.org.bd.
- Evidence files are soft-delete only. No hard deletes.
- File uploads: validate content type, enforce size limits, hash with SHA-256.

### Code Standards
- No writes outside the repo directory.
- No force pushes to main.
- Commits must have clear, descriptive messages. No Co-Authored-By lines.
- Spring Boot: use `@PreAuthorize` for endpoint security. TenantContext for data isolation.
- Frontend: all pages are client components under route groups `(auth)` and `(dashboard)`.
- shadcn/ui v4 uses Base UI — no `asChild` prop. Use `render` prop for trigger customization.
- Select `onValueChange` passes `string | null` — always null-guard.
- Next.js 16: `params` in page components is a `Promise`. Use `use(params)` to unwrap.

### Database
- All schema changes go through Flyway migrations in `backend-spring/src/main/resources/db/migration/`.
- Never use `ddl-auto: update` in production. Only `validate`.
- JSONB columns use `@JdbcTypeCode(SqlTypes.JSON)`. For queries on JSONB fields, use native queries with CAST.
- H2 tests use `MODE=PostgreSQL` for JSONB compatibility.

### AI Service
- LLM calls must have a mock/fallback provider for when no API key is set.
- Crawled domains must be validated against the allowlist before fetching.
- Rate limit: max 2 requests/second to regulator sites.
- All extraction results are sent back to Spring Boot via HTTP callbacks, not returned synchronously.

## Key Directories

```
backend-spring/src/main/java/com/compliance/
  model/          # JPA entities (14 entities, UUID primary keys)
  model/enums/    # 16 enum classes
  repository/     # Spring Data JPA repositories
  service/        # Business logic
  controller/     # REST controllers (9 controllers)
  security/       # JWT, TenantContext, SecurityConfig
  config/         # DataSeeder, SecurityConfig
  audit/          # AuditService

ai-service/app/
  crawler/        # Regulatory site parsers and crawler
  extraction/     # PDF text extraction
  ai/             # LLM obligation extraction
  assistant/      # Compliance Q&A chatbot
  api/            # FastAPI routes
  schemas/        # Pydantic models

frontend/src/
  app/(auth)/     # Login page
  app/(dashboard)/ # All authenticated pages
  components/     # Layout + shadcn/ui components
  lib/            # API client, auth context, hooks
  types/          # TypeScript types matching backend DTOs
```
