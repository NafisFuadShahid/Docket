package com.compliance.repository;

import com.compliance.model.Obligation;
import com.compliance.model.enums.ApplicabilityStatus;
import com.compliance.model.enums.ReviewStatus;
import com.compliance.model.enums.Severity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ObligationRepository extends JpaRepository<Obligation, UUID>, JpaSpecificationExecutor<Obligation> {
    Optional<Obligation> findByIdAndTenantId(UUID id, UUID tenantId);
    Page<Obligation> findByTenantId(UUID tenantId, Pageable pageable);
    List<Obligation> findByTenantIdAndReviewStatus(UUID tenantId, ReviewStatus status);
    List<Obligation> findByCircularId(UUID circularId);

    @Query("SELECT COUNT(o) FROM Obligation o WHERE o.tenantId = :tenantId AND o.reviewStatus = :status")
    long countByTenantIdAndReviewStatus(UUID tenantId, ReviewStatus status);

    @Query("SELECT COUNT(o) FROM Obligation o WHERE o.tenantId = :tenantId AND o.severity = :severity")
    long countByTenantIdAndSeverity(UUID tenantId, Severity severity);

    @Query("SELECT COUNT(o) FROM Obligation o WHERE o.tenantId = :tenantId AND o.applicabilityStatus = :status")
    long countByTenantIdAndApplicabilityStatus(UUID tenantId, ApplicabilityStatus status);

    @Query(value = "SELECT * FROM obligations WHERE tenant_id = :tenantId AND CAST(impacted_departments AS VARCHAR) LIKE CONCAT('%', :dept, '%')", nativeQuery = true)
    List<Obligation> findByTenantIdAndDepartment(UUID tenantId, String dept);
}
