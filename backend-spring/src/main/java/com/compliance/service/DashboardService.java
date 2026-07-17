package com.compliance.service;

import com.compliance.dto.DashboardDtos.*;
import com.compliance.dto.ObligationDtos;
import com.compliance.dto.TaskDtos;
import com.compliance.model.AuditLog;
import com.compliance.model.enums.CircularStatus;
import com.compliance.model.enums.ReviewStatus;
import com.compliance.model.enums.TaskStatus;
import com.compliance.repository.*;
import com.compliance.security.TenantContext;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class DashboardService {

    private final CircularRepository circularRepository;
    private final ObligationRepository obligationRepository;
    private final TaskRepository taskRepository;
    private final EvidenceFileRepository evidenceRepository;
    private final AlertRepository alertRepository;
    private final AuditLogRepository auditLogRepository;
    private final ObligationService obligationService;
    private final TaskService taskService;

    public DashboardService(CircularRepository circularRepository,
                            ObligationRepository obligationRepository,
                            TaskRepository taskRepository,
                            EvidenceFileRepository evidenceRepository,
                            AlertRepository alertRepository,
                            AuditLogRepository auditLogRepository,
                            ObligationService obligationService,
                            TaskService taskService) {
        this.circularRepository = circularRepository;
        this.obligationRepository = obligationRepository;
        this.taskRepository = taskRepository;
        this.evidenceRepository = evidenceRepository;
        this.alertRepository = alertRepository;
        this.auditLogRepository = auditLogRepository;
        this.obligationService = obligationService;
        this.taskService = taskService;
    }

    public OverviewResponse getOverview() {
        UUID tenantId = TenantContext.getTenantId();
        OverviewResponse r = new OverviewResponse();

        r.setTotalCirculars(circularRepository.count());
        r.setPendingReview(obligationRepository.countByTenantIdAndReviewStatus(tenantId, ReviewStatus.PENDING));
        r.setOverdueTasks(taskRepository.countOverdue(tenantId, Instant.now()));
        r.setTotalObligations(obligationRepository.findByTenantId(tenantId, Pageable.unpaged()).getTotalElements());
        r.setTotalTasks(taskRepository.findByTenantId(tenantId, Pageable.unpaged()).getTotalElements());
        r.setCompletedTasks(taskRepository.countByTenantIdAndStatus(tenantId, TaskStatus.COMPLETED));
        r.setUnreadAlerts(alertRepository.countByTenantIdAndIsReadFalse(tenantId));
        r.setEvidenceGaps(computeEvidenceGaps(tenantId));

        long totalTasks = r.getTotalTasks();
        r.setComplianceScore(totalTasks > 0 ? (double) r.getCompletedTasks() / totalTasks * 100 : 100.0);

        r.setObligationStats(obligationService.getDashboardStats());
        r.setTaskStats(taskService.getDashboardStats());
        return r;
    }

    public List<TimelineEntry> getTimeline() {
        UUID tenantId = TenantContext.getTenantId();
        return auditLogRepository.findByTenantIdOrderByCreatedAtDesc(tenantId, PageRequest.of(0, 50))
                .stream().map(this::toTimelineEntry).toList();
    }

    public DepartmentDashboard getDepartmentDashboard(String department) {
        UUID tenantId = TenantContext.getTenantId();
        DepartmentDashboard d = new DepartmentDashboard();
        d.setDepartment(department);

        var tasks = taskRepository.findByTenantIdAndDepartment(tenantId, department);
        d.setTotalObligations(tasks.stream().map(t -> t.getObligationId()).distinct().count());
        d.setPendingTasks(tasks.stream().filter(t -> t.getStatus() == TaskStatus.PENDING).count());
        d.setOverdueTasks(tasks.stream().filter(t ->
                t.getStatus() != TaskStatus.COMPLETED && t.getStatus() != TaskStatus.CANCELLED &&
                t.getDueDate() != null && t.getDueDate().isBefore(Instant.now())).count());
        d.setEvidenceGaps(tasks.stream().filter(t ->
                t.getEvidenceRequired() && evidenceRepository.findByTaskIdAndIsDeletedFalse(t.getId()).isEmpty()).count());
        return d;
    }

    private long computeEvidenceGaps(UUID tenantId) {
        return taskRepository.findByTenantId(tenantId, Pageable.unpaged()).stream()
                .filter(t -> t.getEvidenceRequired() && t.getStatus() != TaskStatus.CANCELLED)
                .filter(t -> evidenceRepository.findByTaskIdAndIsDeletedFalse(t.getId()).isEmpty())
                .count();
    }

    private TimelineEntry toTimelineEntry(AuditLog al) {
        TimelineEntry e = new TimelineEntry();
        e.setId(al.getId());
        e.setType(al.getAction());
        e.setTitle(al.getAction().replace("_", " "));
        e.setDescription(al.getEntityType() + " " + al.getAction().toLowerCase());
        e.setTimestamp(al.getCreatedAt());
        e.setEntityType(al.getEntityType());
        e.setEntityId(al.getEntityId());
        return e;
    }
}
