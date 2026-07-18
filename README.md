# Docket

Regulatory compliance platform for Bangladeshi banks. Crawls circulars from Bangladesh Bank and BFIU, extracts obligations using LLMs, routes them to the right departments, tracks implementation, and generates audit-ready evidence packs.

Built for scheduled banks that need to stay current with BB circulars, BFIU directives, and prudential guidelines without manually reading every PDF.

## How it works

```
Regulator sites ──crawl──> Circulars ──extract──> Obligations ──route──> Departments
     (BB, BFIU)              (PDFs)      (LLM)     (structured)          (tasks, evidence)
```

1. **Crawl** — Scheduled scraping of bb.org.bd and bfiu.org.bd. New circulars are detected, PDFs downloaded, text extracted.
2. **Extract** — LLM (Groq/Llama 3.3 70B) reads the full text and pulls out individual obligations: what must be done, by whom, by when, at what severity.
3. **Review** — Compliance officers review AI-extracted obligations, approve or reject, add notes.
4. **Route** — Approved obligations generate tasks for impacted departments (Treasury, AML/CFT, Credit Risk, etc.).
5. **Evidence** — Departments upload policy documents, SOPs, memos as compliance evidence. Files are SHA-256 hashed and immutable.
6. **Audit** — Generate audit packs per circular: obligations, tasks, evidence, review history — everything an auditor needs in one document.
7. **Ask** — RAG-powered assistant answers compliance questions grounded in your actual circular corpus with citations.

## Architecture

Three services behind Docker Compose:

| Service | Stack | Role |
|---------|-------|------|
| `backend` | Spring Boot 3.4, Java 21, PostgreSQL 16 | Auth, RBAC, CRUD, workflows, audit logging |
| `ai-service` | FastAPI, Python 3.12, LightRAG | Crawling, PDF extraction, LLM obligation extraction, RAG assistant |
| `frontend` | Next.js 16, React 19, shadcn/ui, Tailwind | Enterprise dashboard, 15 pages |

Communication is HTTP. Spring Boot triggers crawl/extraction jobs on the AI service. AI service posts results back via internal callback endpoints. Frontend talks exclusively to Spring Boot.

```
┌──────────┐     ┌──────────────┐     ┌────────────┐
│ Frontend │────>│  Spring Boot │<───>│  PostgreSQL │
│ (Next.js)│     │   (API)      │     │    (16)     │
└──────────┘     └──────┬───────┘     └────────────┘
                        │
                        v
                 ┌──────────────┐
                 │  AI Service  │
                 │  (FastAPI)   │
                 └──────────────┘
```

## Quick start

```bash
# Prerequisites: Docker, Docker Compose

# Clone and start
git clone https://github.com/NafisFuadShahid/Docket.git
cd Docket
docker compose up
```

Three containers come up: PostgreSQL on 5432, backend on 8080, AI service on 8000, frontend on 3000.

On first boot, the backend seeds a demo tenant ("Demo National Bank Ltd") with five users and four regulatory sources.

**Default accounts:**

| Role | Email | Password |
|------|-------|----------|
| Compliance Admin | admin@demo-bank.com | admin123 |
| Reviewer | reviewer@demo-bank.com | reviewer123 |
| Auditor | auditor@demo-bank.com | auditor123 |
| Dept. Owner (Treasury) | treasury@demo-bank.com | treasury123 |
| Dept. Owner (AML/CFT) | aml@demo-bank.com | aml123 |

Open `http://localhost:3000` and sign in.

### AI features

The crawling and extraction pipeline works out of the box. For LLM-powered obligation extraction and the RAG assistant, set API keys:

```bash
GROQ_API_KEY=your-key-here    # Groq (free tier available)
JINA_API_KEY=your-key-here    # Jina Embeddings v3
```

Pass them as environment variables or add to a `.env` file in the project root. Without these keys, the system still crawls and stores circulars — it just skips the AI extraction step.

## Running without Docker

```bash
# Backend
cd backend-spring
./gradlew bootRun

# AI service
cd ai-service
python -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt
uvicorn app.main:app --port 8000

# Frontend
cd frontend
npm install && npm run dev
```

Requires PostgreSQL 16 running locally. See `docker-compose.yml` for the expected database configuration.

## Data model

16 JPA entities, 16 enum types, managed by Flyway migrations.

**Core entities:** Tenant, User, RegulatorySource, Circular, DocumentVersion, ExtractedText, Obligation, ComplianceTask, EvidenceFile, AuditPack, Alert, AuditLog, InstitutionProfile, Conversation, ChatMessage, ApplicabilityOverride.

**Multi-tenancy:** Every query is scoped to the authenticated user's tenant via `TenantContext` (set by `JwtAuthFilter`). Data isolation is enforced at the repository layer.

**RBAC:** Five roles — `COMPLIANCE_ADMIN`, `REVIEWER`, `AUDITOR`, `DEPARTMENT_OWNER`, `VIEWER`. Enforced via Spring Security `@PreAuthorize` annotations on every endpoint.

## Regulatory sources

Preconfigured to crawl:

| Source | Type | URL |
|--------|------|-----|
| Bangladesh Bank Circulars | BB_CIRCULAR | bb.org.bd/mediaroom/circular |
| Bangladesh Bank Guidelines | BB_GUIDELINE | bb.org.bd/about/guidelist |
| Bangladesh Bank Notices | BB_NOTICE | bb.org.bd/mediaroom/noticeboard |
| BFIU Circulars | BFIU_CIRCULAR | bfiu.org.bd/legislation/circular |

Domain allowlist is enforced — the crawler only fetches from `bb.org.bd` and `bfiu.org.bd`. Rate limited to 2 requests/second.

## Frontend pages

| Page | Path | Purpose |
|------|------|---------|
| Dashboard | `/` | Compliance health score, stat cards, obligation/task breakdowns, activity timeline |
| Regulatory Inbox | `/circulars` | All crawled circulars with search, status filter, crawl trigger |
| Circular Detail | `/circulars/[id]` | Full text, metadata, document versions, extraction trigger |
| Review Queue | `/reviews` | Pending obligations for reviewer approval/rejection |
| Obligations | `/obligations` | All extracted obligations with severity, confidence, department routing |
| Departments | `/departments` | Nine department workspaces with per-department stats |
| Department Detail | `/departments/[slug]` | Tasks and obligations scoped to one department |
| Tasks | `/tasks` | Implementation tasks across all departments |
| Task Detail | `/tasks/[id]` | Task progress, evidence uploads, status updates |
| Evidence Vault | `/evidence` | All uploaded compliance evidence with SHA-256 hashes |
| Audit Packs | `/audit-packs` | Generated audit documentation per circular |
| AI Assistant | `/assistant` | RAG-powered compliance Q&A with citations |
| Alerts | `/alerts` | System notifications for new circulars, overdue tasks |
| Settings | `/settings` | User profile, password change, institution configuration |

## API

44 REST endpoints under `/api/v1/`. JWT authentication (access + refresh tokens). All responses follow Spring conventions — paginated endpoints return `Page<T>` objects.

Key endpoint groups:

- `/auth/*` — Login, refresh, profile, user management
- `/circulars/*` — CRUD, search, filter by status, text extraction
- `/sources/*` — Regulatory source management, crawl triggers
- `/obligations/*` — Review workflow (approve/reject/edit), applicability overrides
- `/tasks/*` — Task lifecycle, assignment, status transitions
- `/evidence/*` — Upload (multipart), download, soft-delete
- `/audit-packs/*` — Generation and download
- `/assistant/*` — Chat with conversation history
- `/dashboard/*` — Aggregated overview and department dashboards
- `/alerts/*` — Notification management
- `/internal/*` — AI service callbacks (crawl results, PDF downloads, extraction results)

## Tests

```bash
# Backend (from backend-spring/)
./gradlew test

# AI service (from ai-service/)
source .venv/bin/activate && python -m pytest tests/ -v

# Frontend type check
cd frontend && npx tsc --noEmit
```

## Project structure

```
backend-spring/
  src/main/java/com/compliance/
    model/            16 JPA entities
    model/enums/      16 enum types
    repository/       16 Spring Data repositories
    service/          Business logic layer
    controller/       10 REST controllers
    security/         JWT filter, TenantContext, SecurityConfig
    config/           DataSeeder, RestTemplate config
    audit/            Audit logging service
    dto/              Request/response DTOs
  src/main/resources/
    db/migration/     Flyway SQL migrations

ai-service/app/
    crawler/          Site-specific parsers (BB, BFIU)
    extraction/       PDF text extraction (pdfplumber)
    ai/               LLM obligation extraction (Groq)
    assistant/        RAG chat (LightRAG + Jina embeddings)
    api/              FastAPI route handlers

frontend/src/
    app/(auth)/       Login page
    app/(dashboard)/  14 authenticated pages
    components/       Layout, sidebar, shadcn/ui primitives
    lib/              API client, auth context, hooks
    types/            TypeScript types matching backend DTOs
```

## License

This project is not yet licensed for distribution. All rights reserved.
