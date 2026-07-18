# Production Readiness Review — Docket

**Date:** 2026-07-19
**Reviewers:** Enterprise Architect, Backend/API Engineer, AI/RAG Engineer, Frontend/Product Designer, Security/Compliance Engineer
**Branch reviewed:** `main` at commit `318b3fa` (post-fix commit `9ec3950`)
**Services tested:** Spring Boot backend, FastAPI AI service, Next.js frontend, PostgreSQL 16

---

## Executive Verdict

**Not production-ready. Strong prototype with real regulatory data.**

Docket has genuine substance — 107 live Bangladesh Bank circulars crawled from regulator sites, real PDF extraction, working LLM obligation extraction, and a polished 15-page dashboard. The core pipeline (crawl, extract, review, route, evidence, audit) is functional end-to-end. For a demo to a technical audience, this holds up for 30 minutes with guided navigation.

It is not ready for production use because of unresolved security gaps in multi-tenant isolation, incomplete search and filter implementations on several backend endpoints, and missing test coverage across all three services. The AI extraction pipeline works but silently truncates long documents, which would cause compliance teams to miss obligations from lengthy circulars — a direct risk to the product's core promise.

**Score: 38/100**

| Category | Score | Weight | Weighted |
|----------|-------|--------|----------|
| Core functionality | 55/100 | 25% | 13.8 |
| Security | 25/100 | 25% | 6.3 |
| Data integrity | 40/100 | 20% | 8.0 |
| Frontend completeness | 50/100 | 15% | 7.5 |
| Test coverage | 15/100 | 10% | 1.5 |
| Deployment readiness | 20/100 | 5% | 1.0 |
| **Total** | | | **38.0** |

---

## Feature Inventory

### Backend — 10 Controllers, 44 Endpoints

| Controller | Endpoints | Status |
|------------|-----------|--------|
| AuthController | login, refresh, me, users, change-password | Working. No rate limiting on login. |
| CircularController | list, detail, versions, text, extract, crawl, crawl-all, 3 internal callbacks | Working. Search/filter added in this review. |
| ObligationController | list, detail, review, applicability, 1 internal callback | Working. |
| TaskController | list, detail, create, update-status | Working. |
| EvidenceController | list, upload, download, delete | Working. Path traversal fixed in this review. |
| AuditPackController | list, generate, download | Working. |
| DashboardController | overview, timeline, department | Working. Score formula fixed in this review. |
| AlertController | list, mark-read, mark-all-read, unread-count | Working. |
| InstitutionProfileController | get, update | Working. |
| AssistantController | chat, conversations, conversation-detail | Working. Requires Groq API key. |

### AI Service — 6 Endpoints

| Endpoint | Status |
|----------|--------|
| GET /health | Working. |
| POST /crawl/{source_id} | Working. Crawls bb.org.bd, bfiu.org.bd. |
| POST /download-pdf | Working. Domain-restricted. |
| POST /extract-text | Working. Path traversal fixed in this review. |
| POST /extract-obligations | Working when Groq API key configured. Returns empty without key. |
| POST /assistant/chat | Working. RAG-enriched when LightRAG initialized. |

### Frontend — 15 Pages

| Page | Route | Status |
|------|-------|--------|
| Login | /login | Working. |
| Dashboard | / | Working. Compliance gauge, stat cards, timeline. |
| Regulatory Inbox | /circulars | Working. Search, status filter, crawl-all. |
| Circular Detail | /circulars/[id] | Working. Shows text, versions. |
| Review Queue | /reviews | Working. Quick-approve and detail-review buttons. |
| Obligations | /obligations | Working. Severity badges, department routing. |
| Departments | /departments | Working. 9 department cards. |
| Department Detail | /departments/[slug] | Working. Per-department tasks and stats. |
| Tasks | /tasks | Working. Status badges, assignment. |
| Task Detail | /tasks/[id] | Partial. Upload button has no onClick handler. |
| Evidence Vault | /evidence | Working after fix. Client-side search on filename. |
| Audit Packs | /audit-packs | Working. Generate and download. |
| AI Assistant | /assistant | Working. Defensive response parsing. |
| Alerts | /alerts | Working after fix. Reads Page response correctly. |
| Settings | /settings | Working. Profile, password, institution tabs. |

---

## Test Results

### Backend (Spring Boot)

```
./gradlew test — BUILD SUCCESSFUL
2 test classes, all passing
```

Coverage is minimal. Tests exist for entity mapping and basic service logic. No integration tests for controllers, no test for the security filter chain, no test for the internal API key filter.

### AI Service (Python)

```
7 test files present in tests/
```

Tests cover crawler parsers and basic extraction. No tests for the RAG pipeline, assistant chat, LLM extraction, or route handlers.

### Frontend (TypeScript)

```
npx tsc --noEmit — No errors found
```

Type-safe across all 48 source files. No runtime tests (no Jest, no Playwright, no Cypress).

### Endpoint Verification (Live)

All 44 endpoints return expected HTTP status codes when tested with valid tokens. RBAC enforcement confirmed: AUDITOR role correctly blocked from POST /sources/crawl-all (403).

---

## Findings by Severity

### P0 — Fixed in This Review

| # | Finding | Fix Applied |
|---|---------|-------------|
| 1 | Internal callback endpoints (`/api/v1/internal/**`) were completely unauthenticated. Anyone could inject fake obligations with arbitrary tenant_id. | Added `InternalApiKeyFilter` — validates `X-Internal-Api-Key` header on all internal endpoints. AI service sends the key on every callback. Shared via environment variable. |
| 2 | AI service `/extract-text` accepted arbitrary file paths. An attacker could read any file on the container (`/etc/passwd`, env files). | Added `_is_safe_path()` validation — resolves the path and confirms it starts with the configured storage directory. |
| 3 | Domain allowlist bypass: `hostname.endswith("bb.org.bd")` matched `evil-bb.org.bd`. | Changed to exact match or `.`-prefixed match: `host == d or host.endswith("." + d)`. |
| 4 | Evidence upload path traversal: `file.getOriginalFilename()` was used directly in file path construction. A filename like `../../etc/shadow` would write outside storage. | Added `Paths.get(name).getFileName()` to strip directory components. |
| 5 | Alerts page crashed — frontend expected `Alert[]`, backend returned `Page<Alert>`. | Fixed frontend to `useApi<Page<Alert>>`, reads `data.content`. |
| 6 | Evidence page showed empty — frontend expected `Page<Evidence>`, backend returned `List<EvidenceResponse>`. | Fixed frontend to `useApi<Evidence[]>` with client-side search. |
| 7 | "Crawl All Sources" button returned 400 — sent "all" as UUID. | Added `POST /sources/crawl-all` backend endpoint. Frontend uses it. |
| 8 | Search/filter on circulars was non-functional — backend ignored `search` and `status` query params. | Added `findBySearch`, `findByStatus(pageable)`, and `findByStatusAndSearch` repository queries. Controller accepts both params. |

### P1 — Known, Not Yet Fixed

| # | Finding | Impact | Effort |
|---|---------|--------|--------|
| 1 | **Text truncation at 8,000 characters.** `CircularService.handleExtractionResult` sends `substring(0, 8000)` to the LLM. Bangladesh Bank circulars can be 50+ pages. Obligations in the second half are silently missed. | High — defeats the product's core purpose for long circulars. | Medium — chunk and make multiple LLM calls. |
| 2 | **No login rate limiting.** No brute-force protection on `/api/v1/auth/login`. | High for production. | Low — add Spring Security rate limiter or bucket4j. |
| 3 | **JWT tokens cannot be revoked.** No token blacklist. A leaked token is valid until expiry (30 min access, 7 day refresh). | Medium — standard JWT limitation but risky for banking. | Medium — Redis blacklist on logout. |
| 4 | **No password strength requirements.** Backend accepts any password. Seeded passwords are trivial (`admin123`). | Medium. | Low — add validation in AuthService. |
| 5 | **Task detail upload button is dead.** No `onClick` handler on the evidence upload button in `/tasks/[id]`. | Low — cosmetic. | Low. |
| 6 | **Circular version download button is dead.** The download icon in circular detail doesn't trigger a download. | Low — cosmetic. | Low. |
| 7 | **Department stats are partially hardcoded.** Progress bar defaults to 65% instead of computing from real task data. | Low — misleading but not blocking. | Low. |
| 8 | **Evidence delete has no confirmation.** Clicking the trash icon immediately soft-deletes. | Low. | Low. |
| 9 | **Cross-tenant leaks in list endpoints.** `CircularRepository` has no tenant filter — all tenants see all circulars. `DocumentVersionRepository`, `ExtractedTextRepository`, `AssistantMessageRepository` also lack tenant scoping. This is not exploitable in single-tenant mode but would be a data breach in multi-tenant deployment. | Critical for multi-tenant, irrelevant for single-tenant demo. | High — needs repository-layer refactor. |
| 10 | **No content-type validation on evidence upload.** The `upload` endpoint checks file size but not content type. A user could upload an executable disguised as a PDF. | Medium. | Low — check `contentType` against an allowlist. |

### P2 — Improvement Opportunities

| # | Finding |
|---|---------|
| 1 | RAG initialization race condition — no `asyncio.Lock` on LightRAG setup. Concurrent requests during startup could initialize twice. |
| 2 | No assistant context size limit — a very long conversation could exceed the LLM context window. |
| 3 | No startup warning when AI keys are missing — the system silently degrades to no-op extraction. |
| 4 | PDF download has no file size limit — a malicious or corrupt PDF could fill the container's storage. |
| 5 | No audit log for login attempts — compliance teams need this. |
| 6 | CORS allows all headers (`*`) — should be restricted to specific headers. |
| 7 | `anyRequest().permitAll()` at end of security chain is overly permissive. |
| 8 | Docker Compose hardcodes JWT secret in plaintext. Fine for dev, must be env-var-only in production. |
| 9 | No health check endpoint that validates downstream dependencies (DB, AI service). |
| 10 | Frontend has no loading/error states for failed API calls on several pages — silent failures. |

---

## Architecture Assessment

**What works well:**

- Clean separation of concerns: Spring Boot handles auth/RBAC/CRUD, Python handles crawling/AI, Next.js handles presentation. Services communicate via HTTP callbacks — simple and debuggable.
- Flyway migrations manage schema evolution. Single migration file creates all 16 tables with proper constraints, indexes, and JSONB columns.
- Evidence files are SHA-256 hashed and soft-delete only — correct for compliance audit trail requirements.
- RBAC with 5 roles enforced via `@PreAuthorize` on every controller endpoint.
- Regulatory source configuration is data-driven — adding a new source is a database insert, not a code change.
- Dashboard aggregation queries are efficient — single-pass computation of compliance score, task stats, obligation stats.

**What needs work:**

- Multi-tenant isolation is incomplete. Circulars, document versions, and extracted text are shared across tenants. This is acceptable for a single-tenant deployment but would be a data breach in SaaS mode.
- No background job framework. Crawling and extraction are triggered via REST calls to the AI service. There is no retry logic, no dead letter queue, no visibility into failed jobs. A long-running extraction that times out is silently lost.
- No caching layer. Every dashboard load recomputes stats from the database. Every circular list query hits the database without any result caching.
- The AI service has no authentication — it accepts requests from anyone who can reach port 8000. In Docker Compose this is fine (internal network), but in any other deployment it would be exposed.

---

## Deployment Assessment

Docker Compose works for development. Not suitable for production without:

1. Secrets management — JWT secret and API keys must come from a vault, not environment variables in a compose file.
2. HTTPS termination — no TLS configuration exists.
3. Database backups — no backup strategy.
4. Log aggregation — structured logging is in place (structlog in Python, logback in Java) but no log shipping configuration.
5. Monitoring — Spring Actuator exposes health/metrics/info, but no alerting is configured.
6. Horizontal scaling — single-instance architecture. The AI service is stateless and could scale, but the Spring Boot backend stores files locally.

---

## Verdict for Bangladesh Bank Compliance Teams

A compliance team seeing this in a demo would recognize the domain knowledge: the correct regulatory sources (BB circulars, BFIU directives, BB guidelines, BB notices), the right department taxonomy (AML/CFT, Treasury, Credit Risk, Trade Finance, etc.), the obligation extraction workflow matching their actual review process, and the audit pack concept mapping to their real needs during BB inspections.

The 107 real circulars crawled from bb.org.bd give it credibility that no amount of placeholder data could.

However, they would also notice: search does not work on obligation titles (only circulars were fixed), no way to bulk-approve obligations, no export to Excel (every compliance officer's first question), no deadline tracking with email notifications, and no integration with their existing core banking system. These are table-stakes features for adoption, not nice-to-haves.

**For a demo:** viable with a guided script that avoids the dead buttons and missing features.
**For a pilot:** needs the P1 fixes, especially text truncation, rate limiting, and cross-tenant isolation.
**For production:** needs a full security audit, penetration testing, and the deployment items above. Estimate 6-8 weeks of focused engineering.
