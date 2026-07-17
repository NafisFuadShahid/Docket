package com.compliance.repository;

import com.compliance.model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
    Page<AuditLog> findByTenantIdOrderByCreatedAtDesc(UUID tenantId, Pageable pageable);
    Page<AuditLog> findByTenantIdAndEntityTypeOrderByCreatedAtDesc(UUID tenantId, String entityType, Pageable pageable);
    Page<AuditLog> findByTenantIdAndEntityIdOrderByCreatedAtDesc(UUID tenantId, UUID entityId, Pageable pageable);
}
