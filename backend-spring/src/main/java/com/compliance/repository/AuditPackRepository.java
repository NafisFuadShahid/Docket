package com.compliance.repository;

import com.compliance.model.AuditPack;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuditPackRepository extends JpaRepository<AuditPack, UUID> {
    List<AuditPack> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);
    Optional<AuditPack> findByIdAndTenantId(UUID id, UUID tenantId);
}
