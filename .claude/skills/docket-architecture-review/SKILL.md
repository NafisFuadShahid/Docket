---
name: docket-architecture-review
description: Review architecture decisions against Docket's compliance platform requirements
---

# Docket Architecture Review

Evaluate architectural changes against the platform's design principles.

## Architecture Principles

1. **Tenant isolation**: Row-level filtering via tenantId on every table, never schema-per-tenant
2. **Stateless backend**: JWT auth, no server-side sessions, horizontal scaling ready
3. **Service boundary**: Spring Boot owns data/auth/workflows, Python AI service owns document intelligence
4. **Inter-service**: HTTP callbacks between Spring Boot and AI service, no shared database
5. **Audit trail**: Every state change logged to audit_logs table with before/after snapshots

## Review Process

1. Read the proposed change (PR diff or described change)
2. Check against each principle — does it maintain or violate?
3. Verify tenant isolation: new tables must have tenant_id column and repository queries must filter by it
4. Verify service boundaries: no direct DB access from AI service to Spring Boot's database
5. Check for new dependencies: justify any new library addition
6. Report: compliant / needs-change for each principle, with specific line references
