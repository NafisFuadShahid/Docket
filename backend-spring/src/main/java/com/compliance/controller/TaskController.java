package com.compliance.controller;

import com.compliance.dto.TaskDtos.*;
import com.compliance.security.TenantContext;
import com.compliance.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    public ResponseEntity<Page<TaskResponse>> list(@PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(taskService.listTasks(TenantContext.getTenantId(), pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TaskResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(taskService.getTask(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','COMPLIANCE_ADMIN','REVIEWER','DEPARTMENT_OWNER')")
    public ResponseEntity<TaskResponse> create(@Valid @RequestBody CreateTaskRequest request) {
        return ResponseEntity.ok(taskService.createTask(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','COMPLIANCE_ADMIN','REVIEWER','DEPARTMENT_OWNER')")
    public ResponseEntity<TaskResponse> update(@PathVariable UUID id, @Valid @RequestBody UpdateTaskRequest request) {
        return ResponseEntity.ok(taskService.updateTask(id, request));
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<CommentResponse> addComment(@PathVariable UUID id, @Valid @RequestBody CommentRequest request) {
        return ResponseEntity.ok(taskService.addComment(id, request));
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','COMPLIANCE_ADMIN')")
    public ResponseEntity<TaskResponse> approve(@PathVariable UUID id) {
        return ResponseEntity.ok(taskService.approveTask(id));
    }

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardStats> dashboard() {
        return ResponseEntity.ok(taskService.getDashboardStats());
    }
}
