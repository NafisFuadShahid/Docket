package com.compliance.repository;

import com.compliance.model.EvidenceFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EvidenceFileRepository extends JpaRepository<EvidenceFile, UUID> {
    Optional<EvidenceFile> findByIdAndTenantIdAndIsDeletedFalse(UUID id, UUID tenantId);
    List<EvidenceFile> findByTenantIdAndIsDeletedFalse(UUID tenantId);
    List<EvidenceFile> findByTaskIdAndIsDeletedFalse(UUID taskId);
    List<EvidenceFile> findByObligationIdAndIsDeletedFalse(UUID obligationId);
    long countByTenantIdAndIsDeletedFalse(UUID tenantId);
}
