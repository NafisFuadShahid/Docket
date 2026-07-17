# Bangladesh Banking Compliance Platform — Design Spec

## 1. Architecture

Modular monorepo: `backend/` (FastAPI + Celery), `frontend/` (Next.js + shadcn/ui), shared `fixtures/`, unified `docker-compose.yml`.

- **Backend:** FastAPI (async), SQLAlchemy 2.0, Alembic, Celery + Redis, Pydantic v2
- **Frontend:** Next.js 14 App Router, shadcn/ui, Tailwind CSS, TypeScript
- **DB:** PostgreSQL 16, single database, tenant isolation via `tenant_id` column
- **Queue:** Redis as Celery broker
- **Storage:** Local filesystem abstraction (interface ready for S3)
- **Auth:** JWT (access + refresh tokens), bcrypt password hashing, RBAC middleware

## 2. Data Model

### Tenant & Auth

```
tenants: id, name, slug, institution_type, license_number, settings(jsonb), created_at, updated_at, is_active
users: id, tenant_id(fk), email, hashed_password, full_name, role(enum), department, is_active, created_at, last_login
roles: super_admin, compliance_admin, reviewer, department_owner, auditor, viewer
```

### Institution Profile

```
institution_profiles: id, tenant_id(fk), institution_type(enum), business_lines(jsonb), departments(jsonb), regulators(jsonb), created_at, updated_at
  institution_type enum: scheduled_bank, nbfi, mfs, psp, ad_branch, islamic_banking, agent_banking, digital_lending
  business_lines: foreign_exchange, sme_agri_loans, card_payment, trade_finance, treasury, branch_network, etc.
```

### Regulatory Sources & Documents

```
regulatory_sources: id, name, slug, base_url, source_type(enum: bb_circular, bfiu_circular, bb_guideline, bb_notice), crawl_interval_minutes, is_active, last_crawled_at, created_at
circulars: id, tenant_id(nullable for global), source_id(fk), circular_number, title, title_bn, department, issued_date, effective_date, source_url, language(enum: en, bn, both), status(enum), raw_metadata(jsonb), first_seen_at, last_seen_at, created_at, updated_at
document_versions: id, circular_id(fk), version_number, file_path, file_name, content_type, file_size, sha256_hash, language, download_url, downloaded_at, created_at
extracted_texts: id, document_version_id(fk), full_text, extraction_method(enum: pdf_text, ocr, manual), page_count, chunks(jsonb), extraction_status(enum), error_message, created_at
```

Circular statuses: `detected, downloaded, text_extracted, ai_processed, pending_review, approved, routed, archived, failed`

### Obligations

```
obligations: id, tenant_id(fk), circular_id(fk), obligation_title, obligation_detail, source_quote, source_page, regulator, circular_number, source_department, affected_institution_types(jsonb), affected_business_lines(jsonb), impacted_departments(jsonb), deadline, effective_date, required_actions(jsonb), required_evidence(jsonb), severity(enum: low/medium/high/critical), confidence(float), rationale, ai_model_used, extraction_version, review_status(enum: pending/approved/edited/rejected), reviewed_by(fk), reviewed_at, reviewer_notes, applicability_status(enum: applicable/partially_applicable/not_applicable/needs_review), applicability_reason, applicability_overridden_by(fk), created_at, updated_at
```

### Tasks

```
tasks: id, tenant_id(fk), obligation_id(fk), circular_id(fk), title, description, task_type(enum), owner_id(fk), department, due_date, priority(enum: low/medium/high/critical), status(enum: pending/in_progress/blocked/completed/cancelled), evidence_required(bool), approval_status(enum: not_required/pending/approved/rejected), approved_by(fk), approved_at, created_at, updated_at
task_comments: id, task_id(fk), user_id(fk), content, created_at
task_types: update_policy, update_sop, issue_memo, train_staff, configure_system, submit_report, upload_evidence, legal_review, board_approval, branch_communication
```

### Evidence

```
evidence_files: id, tenant_id(fk), task_id(fk nullable), obligation_id(fk nullable), file_path, file_name, content_type, file_size, sha256_hash, evidence_type(enum), uploaded_by(fk), description, is_deleted(bool), deleted_by(fk), deleted_at, created_at
evidence_types: policy, sop, memo, screenshot, report, training_record, board_memo, system_config_proof, email, circular_acknowledgement
```

### Audit

```
audit_logs: id, tenant_id(fk), user_id(fk), action(str), entity_type(str), entity_id(uuid), old_values(jsonb), new_values(jsonb), ip_address, created_at
audit_packs: id, tenant_id(fk), circular_id(fk), generated_by(fk), pack_data(jsonb), file_path, format(enum: html/pdf), created_at
```

### Alerts

```
alerts: id, tenant_id(fk), user_id(fk nullable), alert_type(enum), title, message, severity(enum), entity_type, entity_id, is_read(bool), read_at, channel(enum: in_app/email/both), created_at
alert_types: new_circular, high_risk_obligation, deadline_approaching, overdue_task, review_required, evidence_gap
```

### AI Assistant

```
assistant_conversations: id, tenant_id(fk), user_id(fk), title, created_at, updated_at
assistant_messages: id, conversation_id(fk), role(enum: user/assistant), content, citations(jsonb), model_used, created_at
```

## 3. API Routes

### Auth
- POST /api/v1/auth/login
- POST /api/v1/auth/refresh
- GET /api/v1/auth/me
- POST /api/v1/auth/change-password

### Tenants & Users
- GET/PUT /api/v1/tenants/current
- GET/POST /api/v1/users
- GET/PUT /api/v1/users/{id}
- GET/PUT /api/v1/institution-profile

### Regulatory Sources
- GET /api/v1/sources
- POST /api/v1/sources/{id}/crawl (trigger manual crawl)
- GET /api/v1/sources/{id}/status

### Circulars (Regulatory Inbox)
- GET /api/v1/circulars (filterable: source, department, language, status, date range)
- GET /api/v1/circulars/{id}
- GET /api/v1/circulars/{id}/versions
- GET /api/v1/circulars/{id}/text
- POST /api/v1/circulars/upload (manual upload fallback)
- POST /api/v1/circulars/{id}/reprocess

### Obligations
- GET /api/v1/obligations (filterable: status, severity, department, deadline, review_status)
- GET /api/v1/obligations/{id}
- PUT /api/v1/obligations/{id}/review (approve/edit/reject)
- PUT /api/v1/obligations/{id}/applicability (override)
- GET /api/v1/obligations/dashboard (stats)

### Tasks
- GET /api/v1/tasks (filterable: department, status, priority, due_date, owner)
- POST /api/v1/tasks
- GET/PUT /api/v1/tasks/{id}
- POST /api/v1/tasks/{id}/comments
- PUT /api/v1/tasks/{id}/approve
- GET /api/v1/tasks/dashboard (stats)

### Evidence
- POST /api/v1/evidence/upload
- GET /api/v1/evidence (filterable: task, obligation, type)
- GET /api/v1/evidence/{id}
- DELETE /api/v1/evidence/{id} (soft delete)
- GET /api/v1/evidence/{id}/download

### Audit
- GET /api/v1/audit-logs (filterable: entity, action, user, date)
- POST /api/v1/audit-packs/generate
- GET /api/v1/audit-packs
- GET /api/v1/audit-packs/{id}/download

### Department Workspaces
- GET /api/v1/departments/{slug}/dashboard
- GET /api/v1/departments/{slug}/obligations
- GET /api/v1/departments/{slug}/tasks
- GET /api/v1/departments/{slug}/evidence-gaps

### AI Assistant
- POST /api/v1/assistant/chat
- GET /api/v1/assistant/conversations
- GET /api/v1/assistant/conversations/{id}

### Alerts
- GET /api/v1/alerts (filterable: type, read status)
- PUT /api/v1/alerts/{id}/read
- PUT /api/v1/alerts/read-all
- GET /api/v1/alerts/unread-count

### Dashboard
- GET /api/v1/dashboard/overview (compliance health, stats)
- GET /api/v1/dashboard/timeline (recent activity)

## 4. Worker/Crawler Design

### Celery Tasks

1. **crawl_source(source_id)** — Fetch source HTML, parse new circulars, detect changes via hash comparison, download PDFs. Allowlisted domains: `bb.org.bd`, `bfiu.org.bd`. Polite rate limit: 2 req/sec. Timeout: 30s per request.

2. **extract_text(document_version_id)** — Extract text from PDF. Try `pdfplumber` first, fall back to OCR stub (marks as `needs_manual_review`). Save chunks with page references.

3. **extract_obligations(circular_id)** — Send extracted text to LLM provider. Parse structured obligations. Schema-validate every output. If no API key, use deterministic mock. Never auto-publish — set `review_status=pending`.

4. **check_applicability(obligation_id)** — Match obligation against tenant institution profile. Set applicability status with explanation.

5. **generate_tasks(obligation_id)** — For approved+applicable obligations, create tasks based on `required_actions`.

6. **send_alerts(alert_type, entity_id)** — Create in-app alert records. Email stub logs to structured logger.

7. **generate_audit_pack(circular_id, tenant_id)** — Collect all data, render HTML, optionally PDF via weasyprint.

### Source Parsers

One parser per source type, each implementing:
```python
class SourceParser(Protocol):
    def parse(self, html: str) -> list[ParsedCircular]: ...
```

Parsers: `BBCircularParser`, `BFIUCircularParser`, `BBGuidelineParser`, `BBNoticeParser`

## 5. Frontend Page Map

### Layout
- Sidebar navigation (collapsible)
- Top bar: tenant name, user menu, notification bell, search
- Main content area with breadcrumbs

### Pages
1. **Dashboard** `/` — compliance health score, stat cards (total circulars, pending reviews, overdue tasks, evidence gaps), recent activity timeline, charts (obligations by severity, tasks by status, compliance trend)
2. **Regulatory Inbox** `/circulars` — filterable table, status badges, bulk actions
3. **Circular Detail** `/circulars/[id]` — document viewer, extracted text, obligation list, versions
4. **Review Queue** `/reviews` — pending obligations, side-by-side source+extraction, approve/edit/reject
5. **Obligations** `/obligations` — filterable table, severity badges, applicability status
6. **Department Workspaces** `/departments/[slug]` — per-department dashboard, obligations, tasks, evidence gaps
7. **Tasks** `/tasks` — kanban or table view, filters, assignment, comments
8. **Task Detail** `/tasks/[id]` — details, comments, evidence links, approval
9. **Evidence Vault** `/evidence` — file list, upload, linked entities, gaps
10. **Audit Packs** `/audit-packs` — generate, download, history
11. **AI Assistant** `/assistant` — chat interface, citations panel
12. **Alerts** `/alerts` — notification list, read/unread, filters
13. **Settings** `/settings` — institution profile, user management, source config
14. **Login** `/login`

## 6. Security Model

- JWT access tokens (30 min) + refresh tokens (7 days)
- bcrypt password hashing, minimum 12 rounds
- `tenant_id` on every business query via middleware dependency
- RBAC checked at route level via dependency
- Audit log for: login, review decisions, task status changes, evidence upload/delete, applicability overrides, user management, audit pack generation
- File upload: validate content type, max 50MB, virus scan stub
- Crawler: allowlisted domains only (`bb.org.bd`, `bfiu.org.bd`), no redirect following to other domains
- No secrets in code — all via env vars
- CORS: configurable origins

## 7. Test Plan

- **Crawler parser tests:** saved HTML fixtures → assert correct circular extraction
- **Hash/versioning:** same title + new hash → new version created
- **Obligation schema:** validate AI output against Pydantic schema
- **Tenant isolation:** user A cannot see tenant B data
- **Applicability matching:** institution profile → correct applicability labels
- **Task generation:** approved+applicable obligation → correct tasks created
- **Audit log:** sensitive actions → log entries created
- **AI mock:** no API key → deterministic mock obligations
- **API permissions:** viewer cannot approve, auditor cannot edit
- **Evidence soft delete:** delete → is_deleted=true, still in DB
