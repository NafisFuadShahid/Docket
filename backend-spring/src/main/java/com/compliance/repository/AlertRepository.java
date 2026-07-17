package com.compliance.repository;

import com.compliance.model.Alert;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface AlertRepository extends JpaRepository<Alert, UUID> {
    Page<Alert> findByTenantIdOrderByCreatedAtDesc(UUID tenantId, Pageable pageable);
    Page<Alert> findByTenantIdAndIsReadOrderByCreatedAtDesc(UUID tenantId, boolean isRead, Pageable pageable);
    Optional<Alert> findByIdAndTenantId(UUID id, UUID tenantId);
    long countByTenantIdAndIsReadFalse(UUID tenantId);

    @Modifying
    @Query("UPDATE Alert a SET a.isRead = true, a.readAt = :now WHERE a.tenantId = :tenantId AND a.isRead = false")
    int markAllRead(UUID tenantId, Instant now);
}
