package com.compliance.controller;

import com.compliance.dto.EvidenceDtos.*;
import com.compliance.service.EvidenceService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/evidence")
public class EvidenceController {

    private final EvidenceService evidenceService;

    public EvidenceController(EvidenceService evidenceService) {
        this.evidenceService = evidenceService;
    }

    @GetMapping
    public ResponseEntity<List<EvidenceResponse>> list(
            @RequestParam(required = false) UUID taskId,
            @RequestParam(required = false) UUID obligationId) {
        if (taskId != null) return ResponseEntity.ok(evidenceService.listByTask(taskId));
        if (obligationId != null) return ResponseEntity.ok(evidenceService.listByObligation(obligationId));
        return ResponseEntity.ok(evidenceService.listEvidence());
    }

    @PostMapping("/upload")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','COMPLIANCE_ADMIN','REVIEWER','DEPARTMENT_OWNER')")
    public ResponseEntity<EvidenceResponse> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) UUID taskId,
            @RequestParam(required = false) UUID obligationId,
            @RequestParam String evidenceType,
            @RequestParam(required = false) String description) {
        UploadRequest req = new UploadRequest();
        req.setTaskId(taskId);
        req.setObligationId(obligationId);
        req.setEvidenceType(evidenceType);
        req.setDescription(description);
        return ResponseEntity.ok(evidenceService.upload(file, req));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> download(@PathVariable UUID id) {
        byte[] data = evidenceService.download(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(data);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','COMPLIANCE_ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        evidenceService.softDelete(id);
        return ResponseEntity.ok().build();
    }
}
