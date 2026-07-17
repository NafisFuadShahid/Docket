# Bangladesh Banking Compliance Platform — Design Spec v2

## 1. Architecture

Hybrid enterprise monorepo: Spring Boot core platform + Python AI service + Next.js frontend.

```
compliance/
├── backend-spring/          # Spring Boot — core compliance platform
│   ├── src/main/java/com/compliance/
│   │   ├── config/          # Security, CORS, JWT, audit config
│   │   ├── model/           # JPA entities
│   │   ├── repository/      # Spring Data JPA repos
│   │   ├── service/         # Business logic
│   │   ├── controller/      # REST controllers
│   │   ├── security/        # JWT filter, RBAC, tenant context
│   │   ├── dto/             # Request/Response DTOs
│   │   └── audit/           # Audit interceptor
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   ├── db/migration/    # Flyway migrations
│   │   └── templates/       # Audit pack HTML templates
│   └── src/test/
├── ai-service/              # Python FastAPI — AI & document intelligence
│   ├── app/
│   │   ├── core/            # Config, logging
│   │   ├── crawler/         # Source parsers, downloader
│   │   ├── extraction/      # PDF text extraction, OCR abstraction
│   │   ├── ai/              # LLM provider, mock, obligation extraction
│   │   ├── assistant/       # RAG-based cited assistant
│   │   ├── schemas/         # Pydantic schemas (shared contracts)
│   │   └── api/             # FastAPI routes
│   ├── tests/
│   │   └── fixtures/        # Saved HTML, sample PDFs
│   └── requirements.txt
├── frontend/                # Next.js + shadcn/ui
├── fixtures/                # Shared sample data
├── docker-compose.yml
└── .env.example
```

### Service Boundaries

**Spring Boot (port 8080)** owns:
- Auth (JWT issue/validate, password management)
- Tenant & user management, RBAC
- Regulatory source registry & circular metadata
- Document version registry (metadata, not file processing)
- Obligation CRUD, review workflow, applicability
- Task management, approval workflow
- Evidence metadata, soft-delete
- Audit logs, audit pack orchestration
- Alerts (CRUD, read/unread)
- Department workspace queries
- Dashboard aggregations
- Calls AI service for: crawl trigger, text extraction, obligation extraction, assistant chat

**Python AI Service (port 8000)** owns:
- Live crawler (HTTP fetch, HTML parsing, PDF download)
- PDF text extraction (pdfplumber + OCR fallback)
- AI obligation extraction (LLM provider + mock)
- RAG assistant (cited answers from ingested documents)
- Source parsers per regulator

### Inter-service Communication
- Spring Boot → AI Service: synchronous HTTP (RestTemplate/WebClient)
- AI Service → Spring Boot: callback HTTP POST to push results (crawl results, extracted obligations)
- Shared PostgreSQL database (both services read/write their respective tables)
- Clean API contracts defined as DTOs/Pydantic schemas

## 2. Data Model

Same as v1 — all tables in single PostgreSQL database. Spring Boot manages migrations via Flyway.

### Tenant & Auth
```
tenants: id(uuid), name, slug(unique), institution_type, license_number, settings(jsonb), is_active, created_at, updated_at
users: id(uuid), tenant_id(fk), email(unique), hashed_password, full_name, role(enum), department, is_active, created_at, last_login
roles: SUPER_ADMIN, COMPLIANCE_ADMIN, REVIEWER, DEPARTMENT_OWNER, AUDITOR, VIEWER
```

### Institution Profile
```
institution_profiles: id(uuid), tenant_id(fk unique), institution_type, business_lines(jsonb), departments(jsonb), regulators(jsonb), created_at, updated_at
```

### Regulatory Sources & Documents
```
regulatory_sources: id(uuid), name, slug(unique), base_url, source_type(enum), crawl_interval_minutes, is_active, last_crawled_at, created_at
circulars: id(uuid), source_id(fk), circular_number, title, title_bn, department, issued_date, effective_date, source_url, language(enum), status(enum), raw_metadata(jsonb), first_seen_at, last_seen_at, created_at, updated_at
document_versions: id(uuid), circular_id(fk), version_number, file_path, file_name, content_type, file_size, sha256_hash, language(enum), download_url, downloaded_at, created_at
extracted_texts: id(uuid), document_version_id(fk unique), full_text, extraction_method(enum), page_count, chunks(jsonb), extraction_status(enum), error_message, created_at
```

### Obligations
```
obligations: id(uuid), tenant_id, circular_id(fk), obligation_title, obligation_detail, source_quote, source_page, regulator, circular_number, source_department, affected_institution_types(jsonb), affected_business_lines(jsonb), impacted_departments(jsonb), deadline, effective_date, required_actions(jsonb), required_evidence(jsonb), severity(enum), confidence(float), rationale, ai_model_used, extraction_version, review_status(enum), reviewed_by, reviewed_at, reviewer_notes, applicability_status(enum), applicability_reason, applicability_overridden_by, created_at, updated_at
```

### Tasks
```
tasks: id(uuid), tenant_id, obligation_id(fk), circular_id(fk), title, description, task_type(enum), owner_id(fk), department, due_date, priority(enum), status(enum), evidence_required, approval_status(enum), approved_by, approved_at, created_at, updated_at
task_comments: id(uuid), task_id(fk), user_id(fk), content, created_at
```

### Evidence
```
evidence_files: id(uuid), tenant_id, task_id(fk nullable), obligation_id(fk nullable), file_path, file_name, content_type, file_size, sha256_hash, evidence_type(enum), uploaded_by(fk), description, is_deleted, deleted_by, deleted_at, created_at
```

### Audit
```
audit_logs: id(uuid), tenant_id, user_id, action, entity_type, entity_id, old_values(jsonb), new_values(jsonb), ip_address, created_at
audit_packs: id(uuid), tenant_id, circular_id, generated_by, title, pack_data(jsonb), file_path, format, created_at
```

### Alerts
```
alerts: id(uuid), tenant_id, user_id(fk nullable), alert_type(enum), title, message, severity(enum), entity_type, entity_id, is_read, read_at, channel(enum), created_at
```

### AI Assistant
```
assistant_conversations: id(uuid), tenant_id, user_id(fk), title, created_at, updated_at
assistant_messages: id(uuid), conversation_id(fk), role(enum), content, citations(jsonb), model_used, created_at
```

## 3. API Routes

All routes served by Spring Boot at `/api/v1/` except AI-specific endpoints.

### Spring Boot APIs (port 8080)
Same route structure as v1:
- Auth: login, refresh, me, change-password
- Tenants & Users: CRUD
- Institution Profile: get/update
- Regulatory Sources: list, trigger crawl (proxied to AI service)
- Circulars: list, detail, versions, text, upload, reprocess
- Obligations: list, detail, review, applicability, dashboard stats
- Tasks: CRUD, comments, approve, dashboard stats
- Evidence: upload, list, detail, soft-delete, download
- Audit: logs, pack generation, pack list, download
- Departments: dashboard, obligations, tasks, evidence-gaps per department
- Alerts: list, read, read-all, unread-count
- Dashboard: overview, timeline
- Assistant: proxied to AI service

### AI Service APIs (port 8000, internal only)
- POST /crawl/{source_id} — trigger crawl
- POST /extract-text — extract text from PDF
- POST /extract-obligations — AI extraction from text
- POST /check-applicability — check obligation against profile
- POST /assistant/chat — cited assistant response
- GET /health — health check

## 4. Worker Design

### AI Service Background Tasks
Python AI service handles async work via background threads or simple task queue:
- **Crawl source:** HTTP fetch with allowlisted domains, rate limiting, timeout
- **Text extraction:** pdfplumber + OCR fallback
- **AI extraction:** LLM call with schema validation, mock when no key

### Spring Boot Scheduled Tasks
- **Periodic crawl trigger:** @Scheduled every 15 min, calls AI service for each active source
- **Deadline checker:** @Scheduled daily, creates overdue alerts
- **Digest generator:** @Scheduled daily, creates daily digest alerts

## 5. Frontend Page Map

Same as v1 — 14 pages with enterprise banking SaaS UI.

## 6. Security Model

Same as v1 but implemented in Spring Security:
- JWT filter chain
- `@PreAuthorize` for RBAC
- Tenant context via `TenantFilter` on all queries
- Hibernate event listener for audit logging
- File upload validation via Spring MultipartFile

## 7. Test Plan

### Spring Boot Tests
- Controller integration tests with MockMvc
- Service unit tests
- Tenant isolation tests
- RBAC permission tests
- Audit log creation tests
- Task/evidence workflow tests

### Python AI Service Tests
- Crawler parser tests with HTML fixtures
- Hash/versioning tests
- Obligation schema validation
- AI mock provider tests
- Text extraction tests
