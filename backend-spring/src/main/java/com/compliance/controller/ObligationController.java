package com.compliance.controller;

import com.compliance.dto.ObligationDtos.*;
import com.compliance.security.TenantContext;
import com.compliance.service.ObligationService;
import com.compliance.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/obligations")
public class ObligationController {

    private final ObligationService obligationService;
    private final TaskService taskService;

    public ObligationController(ObligationService obligationService, TaskService taskService) {
        this.obligationService = obligationService;
        this.taskService = taskService;
    }

    @GetMapping
    public ResponseEntity<Page<ObligationResponse>> list(@PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(obligationService.listObligations(TenantContext.getTenantId(), pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ObligationResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(obligationService.getObligation(id));
    }

    @GetMapping("/pending-review")
    public ResponseEntity<List<ObligationResponse>> pendingReview() {
        return ResponseEntity.ok(obligationService.getPendingReview());
    }

    @PutMapping("/{id}/review")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','COMPLIANCE_ADMIN','REVIEWER')")
    public ResponseEntity<ObligationResponse> review(@PathVariable UUID id, @Valid @RequestBody ReviewRequest request) {
        ObligationResponse r = obligationService.reviewObligation(id, request);
        if ("approve".equalsIgnoreCase(request.getAction()) || "edit".equalsIgnoreCase(request.getAction())) {
            taskService.generateTasksForObligation(id);
        }
        return ResponseEntity.ok(r);
    }

    @PutMapping("/{id}/applicability")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','COMPLIANCE_ADMIN','REVIEWER')")
    public ResponseEntity<ObligationResponse> overrideApplicability(
            @PathVariable UUID id, @Valid @RequestBody ApplicabilityOverrideRequest request) {
        return ResponseEntity.ok(obligationService.overrideApplicability(id, request));
    }

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardStats> dashboard() {
        return ResponseEntity.ok(obligationService.getDashboardStats());
    }

    @PostMapping("/internal/extraction-callback")
    public ResponseEntity<Void> handleExtraction(@RequestBody ObligationExtractionCallback callback) {
        obligationService.handleExtractionCallback(callback);
        return ResponseEntity.ok().build();
    }
}
