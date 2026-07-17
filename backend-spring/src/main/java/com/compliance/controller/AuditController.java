package com.compliance.controller;

import com.compliance.model.AuditLog;
import com.compliance.model.AuditPack;
import com.compliance.repository.AuditLogRepository;
import com.compliance.security.TenantContext;
import com.compliance.service.AuditPackService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class AuditController {

    private final AuditLogRepository auditLogRepository;
    private final AuditPackService auditPackService;

    public AuditController(AuditLogRepository auditLogRepository, AuditPackService auditPackService) {
        this.auditLogRepository = auditLogRepository;
        this.auditPackService = auditPackService;
    }

    @GetMapping("/audit-logs")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','COMPLIANCE_ADMIN','AUDITOR')")
    public ResponseEntity<Page<AuditLog>> listLogs(
            @RequestParam(required = false) String entityType,
            @PageableDefault(size = 50) Pageable pageable) {
        UUID tenantId = TenantContext.getTenantId();
        if (entityType != null) {
            return ResponseEntity.ok(auditLogRepository.findByTenantIdAndEntityTypeOrderByCreatedAtDesc(tenantId, entityType, pageable));
        }
        return ResponseEntity.ok(auditLogRepository.findByTenantIdOrderByCreatedAtDesc(tenantId, pageable));
    }

    @PostMapping("/audit-packs/generate")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','COMPLIANCE_ADMIN','AUDITOR')")
    public ResponseEntity<AuditPack> generate(@RequestBody Map<String, UUID> body) {
        UUID circularId = body.get("circularId");
        return ResponseEntity.ok(auditPackService.generate(circularId));
    }

    @GetMapping("/audit-packs")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','COMPLIANCE_ADMIN','AUDITOR')")
    public ResponseEntity<List<AuditPack>> listPacks() {
        return ResponseEntity.ok(auditPackService.listPacks());
    }

    @GetMapping("/audit-packs/{id}/download")
    public ResponseEntity<byte[]> downloadPack(@PathVariable UUID id) {
        byte[] data = auditPackService.download(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=audit-pack.html")
                .contentType(MediaType.TEXT_HTML)
                .body(data);
    }
}
