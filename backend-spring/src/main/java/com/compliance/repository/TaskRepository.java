package com.compliance.repository;

import com.compliance.model.Task;
import com.compliance.model.enums.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID>, JpaSpecificationExecutor<Task> {
    Optional<Task> findByIdAndTenantId(UUID id, UUID tenantId);
    Page<Task> findByTenantId(UUID tenantId, Pageable pageable);
    List<Task> findByTenantIdAndDepartment(UUID tenantId, String department);
    List<Task> findByObligationId(UUID obligationId);

    @Query("SELECT COUNT(t) FROM Task t WHERE t.tenantId = :tenantId AND t.status = :status")
    long countByTenantIdAndStatus(UUID tenantId, TaskStatus status);

    @Query("SELECT t FROM Task t WHERE t.tenantId = :tenantId AND t.status NOT IN ('COMPLETED','CANCELLED') AND t.dueDate < :now")
    List<Task> findOverdue(UUID tenantId, Instant now);

    @Query("SELECT COUNT(t) FROM Task t WHERE t.tenantId = :tenantId AND t.status NOT IN ('COMPLETED','CANCELLED') AND t.dueDate < :now")
    long countOverdue(UUID tenantId, Instant now);
}
