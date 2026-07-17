package com.compliance.service;

import com.compliance.audit.AuditService;
import com.compliance.dto.TaskDtos.*;
import com.compliance.model.Obligation;
import com.compliance.model.Task;
import com.compliance.model.TaskComment;
import com.compliance.model.enums.*;
import com.compliance.repository.*;
import com.compliance.security.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class TaskService {

    private static final Logger log = LoggerFactory.getLogger(TaskService.class);

    private final TaskRepository taskRepository;
    private final TaskCommentRepository commentRepository;
    private final ObligationRepository obligationRepository;
    private final CircularRepository circularRepository;
    private final EvidenceFileRepository evidenceRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public TaskService(TaskRepository taskRepository, TaskCommentRepository commentRepository,
                       ObligationRepository obligationRepository, CircularRepository circularRepository,
                       EvidenceFileRepository evidenceRepository, UserRepository userRepository,
                       AuditService auditService) {
        this.taskRepository = taskRepository;
        this.commentRepository = commentRepository;
        this.obligationRepository = obligationRepository;
        this.circularRepository = circularRepository;
        this.evidenceRepository = evidenceRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    public Page<TaskResponse> listTasks(UUID tenantId, Pageable pageable) {
        return taskRepository.findByTenantId(tenantId, pageable).map(this::toResponse);
    }

    public TaskResponse getTask(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        Task t = taskRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        TaskResponse r = toResponse(t);
        r.setComments(commentRepository.findByTaskIdOrderByCreatedAtAsc(id).stream().map(this::toCommentResponse).toList());
        return r;
    }

    @Transactional
    public TaskResponse createTask(CreateTaskRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        Task t = Task.builder()
                .tenantId(tenantId)
                .title(request.getTitle())
                .description(request.getDescription())
                .taskType(TaskType.valueOf(request.getTaskType()))
                .ownerId(request.getOwnerId())
                .department(request.getDepartment())
                .dueDate(request.getDueDate())
                .priority(request.getPriority() != null ? Priority.valueOf(request.getPriority()) : Priority.MEDIUM)
                .evidenceRequired(request.getEvidenceRequired() != null ? request.getEvidenceRequired() : false)
                .obligationId(request.getObligationId())
                .circularId(request.getCircularId())
                .build();
        taskRepository.save(t);
        auditService.log("CREATE_TASK", "task", t.getId());
        log.info("task_created id={} dept={} type={}", t.getId(), t.getDepartment(), t.getTaskType());
        return toResponse(t);
    }

    @Transactional
    public TaskResponse updateTask(UUID id, UpdateTaskRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        Task t = taskRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        Map<String, Object> old = Map.of("status", t.getStatus().name());
        if (request.getTitle() != null) t.setTitle(request.getTitle());
        if (request.getDescription() != null) t.setDescription(request.getDescription());
        if (request.getOwnerId() != null) t.setOwnerId(request.getOwnerId());
        if (request.getDepartment() != null) t.setDepartment(request.getDepartment());
        if (request.getDueDate() != null) t.setDueDate(request.getDueDate());
        if (request.getPriority() != null) t.setPriority(Priority.valueOf(request.getPriority()));
        if (request.getStatus() != null) t.setStatus(TaskStatus.valueOf(request.getStatus()));
        if (request.getEvidenceRequired() != null) t.setEvidenceRequired(request.getEvidenceRequired());
        taskRepository.save(t);

        auditService.log("UPDATE_TASK", "task", id, old, Map.of("status", t.getStatus().name()));
        return toResponse(t);
    }

    @Transactional
    public TaskResponse approveTask(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        UUID userId = TenantContext.getUserId();
        Task t = taskRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        t.setApprovalStatus(ApprovalStatus.APPROVED);
        t.setApprovedBy(userId);
        t.setApprovedAt(Instant.now());
        taskRepository.save(t);
        auditService.log("APPROVE_TASK", "task", id);
        return toResponse(t);
    }

    public CommentResponse addComment(UUID taskId, CommentRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        UUID userId = TenantContext.getUserId();
        taskRepository.findByIdAndTenantId(taskId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        TaskComment c = TaskComment.builder()
                .taskId(taskId)
                .userId(userId)
                .content(request.getContent())
                .build();
        commentRepository.save(c);
        return toCommentResponse(c);
    }

    @Transactional
    public void generateTasksForObligation(UUID obligationId) {
        Obligation o = obligationRepository.findById(obligationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (o.getReviewStatus() != ReviewStatus.APPROVED && o.getReviewStatus() != ReviewStatus.EDITED) return;
        if (o.getApplicabilityStatus() != ApplicabilityStatus.APPLICABLE &&
            o.getApplicabilityStatus() != ApplicabilityStatus.PARTIALLY_APPLICABLE) return;

        List<String> departments = o.getImpactedDepartments();
        if (departments.isEmpty()) departments = List.of("compliance");

        for (String action : o.getRequiredActions()) {
            TaskType type = mapActionToType(action);
            for (String dept : departments) {
                Task t = Task.builder()
                        .tenantId(o.getTenantId())
                        .obligationId(o.getId())
                        .circularId(o.getCircularId())
                        .title(action)
                        .description("From obligation: " + o.getObligationTitle())
                        .taskType(type)
                        .department(dept)
                        .dueDate(o.getDeadline())
                        .priority(o.getSeverity() == Severity.CRITICAL ? Priority.CRITICAL :
                                  o.getSeverity() == Severity.HIGH ? Priority.HIGH : Priority.MEDIUM)
                        .evidenceRequired(true)
                        .build();
                taskRepository.save(t);
            }
        }
        log.info("tasks_generated obligation={} count={}", obligationId, o.getRequiredActions().size() * departments.size());
    }

    public DashboardStats getDashboardStats() {
        UUID tenantId = TenantContext.getTenantId();
        DashboardStats stats = new DashboardStats();
        stats.setTotalTasks(taskRepository.findByTenantId(tenantId, Pageable.unpaged()).getTotalElements());
        stats.setPending(taskRepository.countByTenantIdAndStatus(tenantId, TaskStatus.PENDING));
        stats.setInProgress(taskRepository.countByTenantIdAndStatus(tenantId, TaskStatus.IN_PROGRESS));
        stats.setCompleted(taskRepository.countByTenantIdAndStatus(tenantId, TaskStatus.COMPLETED));
        stats.setBlocked(taskRepository.countByTenantIdAndStatus(tenantId, TaskStatus.BLOCKED));
        stats.setOverdue(taskRepository.countOverdue(tenantId, Instant.now()));
        return stats;
    }

    public List<TaskResponse> getTasksByDepartment(String department) {
        UUID tenantId = TenantContext.getTenantId();
        return taskRepository.findByTenantIdAndDepartment(tenantId, department)
                .stream().map(this::toResponse).toList();
    }

    private TaskType mapActionToType(String action) {
        String lower = action.toLowerCase();
        if (lower.contains("policy")) return TaskType.UPDATE_POLICY;
        if (lower.contains("sop")) return TaskType.UPDATE_SOP;
        if (lower.contains("memo")) return TaskType.ISSUE_MEMO;
        if (lower.contains("train")) return TaskType.TRAIN_STAFF;
        if (lower.contains("system") || lower.contains("config")) return TaskType.CONFIGURE_SYSTEM;
        if (lower.contains("report")) return TaskType.SUBMIT_REPORT;
        if (lower.contains("legal")) return TaskType.LEGAL_REVIEW;
        if (lower.contains("board")) return TaskType.BOARD_APPROVAL;
        if (lower.contains("branch")) return TaskType.BRANCH_COMMUNICATION;
        return TaskType.UPLOAD_EVIDENCE;
    }

    private TaskResponse toResponse(Task t) {
        TaskResponse r = new TaskResponse();
        r.setId(t.getId());
        r.setTenantId(t.getTenantId());
        r.setObligationId(t.getObligationId());
        r.setCircularId(t.getCircularId());
        r.setTitle(t.getTitle());
        r.setDescription(t.getDescription());
        r.setTaskType(t.getTaskType().name());
        r.setOwnerId(t.getOwnerId());
        r.setDepartment(t.getDepartment());
        r.setDueDate(t.getDueDate());
        r.setPriority(t.getPriority().name());
        r.setStatus(t.getStatus().name());
        r.setEvidenceRequired(t.getEvidenceRequired());
        r.setApprovalStatus(t.getApprovalStatus().name());
        r.setApprovedBy(t.getApprovedBy());
        r.setApprovedAt(t.getApprovedAt());
        r.setCreatedAt(t.getCreatedAt());
        r.setUpdatedAt(t.getUpdatedAt());

        if (t.getOwnerId() != null) {
            userRepository.findById(t.getOwnerId()).ifPresent(u -> r.setOwnerName(u.getFullName()));
        }
        if (t.getObligationId() != null) {
            obligationRepository.findById(t.getObligationId()).ifPresent(o -> r.setObligationTitle(o.getObligationTitle()));
        }
        if (t.getCircularId() != null) {
            circularRepository.findById(t.getCircularId()).ifPresent(c -> r.setCircularTitle(c.getTitle()));
        }
        r.setEvidenceCount((int) evidenceRepository.findByTaskIdAndIsDeletedFalse(t.getId()).size());
        return r;
    }

    private CommentResponse toCommentResponse(TaskComment c) {
        CommentResponse r = new CommentResponse();
        r.setId(c.getId());
        r.setUserId(c.getUserId());
        r.setContent(c.getContent());
        r.setCreatedAt(c.getCreatedAt());
        userRepository.findById(c.getUserId()).ifPresent(u -> r.setUserName(u.getFullName()));
        return r;
    }
}
