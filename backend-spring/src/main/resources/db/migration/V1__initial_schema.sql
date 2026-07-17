CREATE TABLE tenants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(100) NOT NULL UNIQUE,
    institution_type VARCHAR(100),
    license_number VARCHAR(100),
    settings JSONB DEFAULT '{}',
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    email VARCHAR(255) NOT NULL UNIQUE,
    hashed_password VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'VIEWER',
    department VARCHAR(100),
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_login TIMESTAMPTZ
);
CREATE INDEX idx_users_tenant ON users(tenant_id);
CREATE INDEX idx_users_email ON users(email);

CREATE TABLE institution_profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL UNIQUE REFERENCES tenants(id),
    institution_type VARCHAR(100) NOT NULL,
    business_lines JSONB DEFAULT '[]',
    departments JSONB DEFAULT '[]',
    regulators JSONB DEFAULT '[]',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE regulatory_sources (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(100) NOT NULL UNIQUE,
    base_url VARCHAR(500) NOT NULL,
    source_type VARCHAR(50) NOT NULL,
    crawl_interval_minutes INTEGER DEFAULT 15,
    is_active BOOLEAN NOT NULL DEFAULT true,
    last_crawled_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE circulars (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    source_id UUID NOT NULL REFERENCES regulatory_sources(id),
    circular_number VARCHAR(100),
    title TEXT NOT NULL,
    title_bn TEXT,
    department VARCHAR(255),
    issued_date TIMESTAMPTZ,
    effective_date TIMESTAMPTZ,
    source_url VARCHAR(1000),
    language VARCHAR(10) NOT NULL DEFAULT 'EN',
    status VARCHAR(50) NOT NULL DEFAULT 'DETECTED',
    raw_metadata JSONB DEFAULT '{}',
    first_seen_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_seen_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_circulars_source ON circulars(source_id);
CREATE INDEX idx_circulars_status ON circulars(status);
CREATE INDEX idx_circulars_number ON circulars(circular_number);

CREATE TABLE document_versions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    circular_id UUID NOT NULL REFERENCES circulars(id),
    version_number INTEGER NOT NULL DEFAULT 1,
    file_path VARCHAR(500) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) DEFAULT 'application/pdf',
    file_size INTEGER,
    sha256_hash VARCHAR(64) NOT NULL,
    language VARCHAR(10) NOT NULL DEFAULT 'EN',
    download_url VARCHAR(1000),
    downloaded_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_docver_circular ON document_versions(circular_id);
CREATE INDEX idx_docver_hash ON document_versions(sha256_hash);

CREATE TABLE extracted_texts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_version_id UUID NOT NULL UNIQUE REFERENCES document_versions(id),
    full_text TEXT,
    extraction_method VARCHAR(20) DEFAULT 'PDF_TEXT',
    page_count INTEGER,
    chunks JSONB DEFAULT '[]',
    extraction_status VARCHAR(30) DEFAULT 'PENDING',
    error_message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE obligations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    circular_id UUID NOT NULL REFERENCES circulars(id),
    obligation_title VARCHAR(500) NOT NULL,
    obligation_detail TEXT NOT NULL,
    source_quote TEXT,
    source_page INTEGER,
    regulator VARCHAR(100) NOT NULL,
    circular_number VARCHAR(100),
    source_department VARCHAR(255),
    affected_institution_types JSONB DEFAULT '[]',
    affected_business_lines JSONB DEFAULT '[]',
    impacted_departments JSONB DEFAULT '[]',
    deadline TIMESTAMPTZ,
    effective_date TIMESTAMPTZ,
    required_actions JSONB DEFAULT '[]',
    required_evidence JSONB DEFAULT '[]',
    severity VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    confidence DOUBLE PRECISION DEFAULT 0.0,
    rationale TEXT,
    ai_model_used VARCHAR(100),
    extraction_version VARCHAR(50),
    review_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    reviewed_by UUID,
    reviewed_at TIMESTAMPTZ,
    reviewer_notes TEXT,
    applicability_status VARCHAR(30) NOT NULL DEFAULT 'NEEDS_REVIEW',
    applicability_reason TEXT,
    applicability_overridden_by UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_obligations_tenant ON obligations(tenant_id);
CREATE INDEX idx_obligations_circular ON obligations(circular_id);
CREATE INDEX idx_obligations_review ON obligations(review_status);
CREATE INDEX idx_obligations_applicability ON obligations(applicability_status);

CREATE TABLE tasks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    obligation_id UUID REFERENCES obligations(id),
    circular_id UUID REFERENCES circulars(id),
    title VARCHAR(500) NOT NULL,
    description TEXT,
    task_type VARCHAR(30) NOT NULL,
    owner_id UUID REFERENCES users(id),
    department VARCHAR(100) NOT NULL,
    due_date TIMESTAMPTZ,
    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    evidence_required BOOLEAN DEFAULT false,
    approval_status VARCHAR(20) DEFAULT 'NOT_REQUIRED',
    approved_by UUID,
    approved_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_tasks_tenant ON tasks(tenant_id);
CREATE INDEX idx_tasks_obligation ON tasks(obligation_id);
CREATE INDEX idx_tasks_department ON tasks(department);
CREATE INDEX idx_tasks_status ON tasks(status);

CREATE TABLE task_comments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    task_id UUID NOT NULL REFERENCES tasks(id),
    user_id UUID NOT NULL REFERENCES users(id),
    content TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_taskcomments_task ON task_comments(task_id);

CREATE TABLE evidence_files (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    task_id UUID REFERENCES tasks(id),
    obligation_id UUID REFERENCES obligations(id),
    file_path VARCHAR(500) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_size INTEGER NOT NULL,
    sha256_hash VARCHAR(64) NOT NULL,
    evidence_type VARCHAR(30) NOT NULL,
    uploaded_by UUID NOT NULL REFERENCES users(id),
    description TEXT,
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    deleted_by UUID,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_evidence_tenant ON evidence_files(tenant_id);
CREATE INDEX idx_evidence_task ON evidence_files(task_id);
CREATE INDEX idx_evidence_obligation ON evidence_files(obligation_id);

CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    user_id UUID,
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id UUID,
    old_values JSONB,
    new_values JSONB,
    ip_address VARCHAR(45),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_audit_tenant ON audit_logs(tenant_id);
CREATE INDEX idx_audit_entity ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_action ON audit_logs(action);

CREATE TABLE audit_packs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    circular_id UUID,
    generated_by UUID NOT NULL,
    title VARCHAR(500) NOT NULL DEFAULT 'Audit Pack',
    pack_data JSONB DEFAULT '{}',
    file_path VARCHAR(500),
    format VARCHAR(10) NOT NULL DEFAULT 'html',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_auditpack_tenant ON audit_packs(tenant_id);

CREATE TABLE alerts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    user_id UUID REFERENCES users(id),
    alert_type VARCHAR(30) NOT NULL,
    title VARCHAR(500) NOT NULL,
    message TEXT NOT NULL,
    severity VARCHAR(20) NOT NULL DEFAULT 'INFO',
    entity_type VARCHAR(100),
    entity_id UUID,
    is_read BOOLEAN NOT NULL DEFAULT false,
    read_at TIMESTAMPTZ,
    channel VARCHAR(20) NOT NULL DEFAULT 'in_app',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_alerts_tenant ON alerts(tenant_id);
CREATE INDEX idx_alerts_user ON alerts(user_id);
CREATE INDEX idx_alerts_read ON alerts(is_read);

CREATE TABLE assistant_conversations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    user_id UUID NOT NULL REFERENCES users(id),
    title VARCHAR(500) NOT NULL DEFAULT 'New Conversation',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_assistant_conv_tenant ON assistant_conversations(tenant_id);

CREATE TABLE assistant_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id UUID NOT NULL REFERENCES assistant_conversations(id),
    role VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    citations JSONB DEFAULT '[]',
    model_used VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_assistant_msg_conv ON assistant_messages(conversation_id);
