package com.compliance.service;

import com.compliance.audit.AuditService;
import com.compliance.model.*;
import com.compliance.model.enums.CircularStatus;
import com.compliance.repository.*;
import com.compliance.security.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;

@Service
public class AuditPackService {

    private static final Logger log = LoggerFactory.getLogger(AuditPackService.class);

    private final AuditPackRepository auditPackRepository;
    private final CircularRepository circularRepository;
    private final ObligationRepository obligationRepository;
    private final TaskRepository taskRepository;
    private final EvidenceFileRepository evidenceRepository;
    private final AuditLogRepository auditLogRepository;
    private final DocumentVersionRepository docVersionRepository;
    private final AuditService auditService;
    private final TemplateEngine templateEngine;

    @Value("${app.storage.local-path}")
    private String storagePath;

    public AuditPackService(AuditPackRepository auditPackRepository, CircularRepository circularRepository,
                            ObligationRepository obligationRepository, TaskRepository taskRepository,
                            EvidenceFileRepository evidenceRepository, AuditLogRepository auditLogRepository,
                            DocumentVersionRepository docVersionRepository, AuditService auditService,
                            TemplateEngine templateEngine) {
        this.auditPackRepository = auditPackRepository;
        this.circularRepository = circularRepository;
        this.obligationRepository = obligationRepository;
        this.taskRepository = taskRepository;
        this.evidenceRepository = evidenceRepository;
        this.auditLogRepository = auditLogRepository;
        this.docVersionRepository = docVersionRepository;
        this.auditService = auditService;
        this.templateEngine = templateEngine;
    }

    public AuditPack generate(UUID circularId) {
        UUID tenantId = TenantContext.getTenantId();
        UUID userId = TenantContext.getUserId();

        Circular circular = circularRepository.findById(circularId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        List<Obligation> obligations = obligationRepository.findByCircularId(circularId).stream()
                .filter(o -> o.getTenantId().equals(tenantId)).toList();

        List<UUID> obligationIds = obligations.stream().map(Obligation::getId).toList();
        List<Task> tasks = obligationIds.stream()
                .flatMap(oid -> taskRepository.findByObligationId(oid).stream())
                .filter(t -> t.getTenantId().equals(tenantId)).toList();

        List<EvidenceFile> evidence = obligationIds.stream()
                .flatMap(oid -> evidenceRepository.findByObligationIdAndIsDeletedFalse(oid).stream())
                .filter(e -> e.getTenantId().equals(tenantId)).toList();

        List<DocumentVersion> versions = docVersionRepository.findByCircularIdOrderByVersionNumberDesc(circularId);

        Map<String, Object> packData = new LinkedHashMap<>();
        packData.put("circularNumber", circular.getCircularNumber());
        packData.put("circularTitle", circular.getTitle());
        packData.put("issuedDate", String.valueOf(circular.getIssuedDate()));
        packData.put("sourceUrl", circular.getSourceUrl());
        packData.put("status", circular.getStatus().name());
        packData.put("obligationCount", obligations.size());
        packData.put("taskCount", tasks.size());
        packData.put("evidenceCount", evidence.size());
        packData.put("documentVersions", versions.size());
        packData.put("generatedAt", Instant.now().toString());

        long completedTasks = tasks.stream().filter(t -> t.getStatus() == com.compliance.model.enums.TaskStatus.COMPLETED).count();
        long evidenceGaps = tasks.stream()
                .filter(t -> t.getEvidenceRequired() && evidenceRepository.findByTaskIdAndIsDeletedFalse(t.getId()).isEmpty())
                .count();
        packData.put("completedTasks", completedTasks);
        packData.put("evidenceGaps", evidenceGaps);

        Context ctx = new Context();
        ctx.setVariable("circular", circular);
        ctx.setVariable("obligations", obligations);
        ctx.setVariable("tasks", tasks);
        ctx.setVariable("evidence", evidence);
        ctx.setVariable("versions", versions);
        ctx.setVariable("packData", packData);
        String html = templateEngine.process("audit-pack", ctx);

        String dir = storagePath + "/audit-packs/" + tenantId;
        try {
            Files.createDirectories(Paths.get(dir));
            String filePath = dir + "/audit-pack-" + circularId + "-" + System.currentTimeMillis() + ".html";
            Files.writeString(Paths.get(filePath), html);

            AuditPack pack = AuditPack.builder()
                    .tenantId(tenantId)
                    .circularId(circularId)
                    .generatedBy(userId)
                    .title("Audit Pack: " + circular.getTitle())
                    .packData(packData)
                    .filePath(filePath)
                    .format("html")
                    .build();
            auditPackRepository.save(pack);
            auditService.log("GENERATE_AUDIT_PACK", "audit_pack", pack.getId());
            log.info("audit_pack_generated circular={} pack={}", circularId, pack.getId());
            return pack;
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to generate audit pack");
        }
    }

    public List<AuditPack> listPacks() {
        return auditPackRepository.findByTenantIdOrderByCreatedAtDesc(TenantContext.getTenantId());
    }

    public byte[] download(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        AuditPack pack = auditPackRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        try {
            return Files.readAllBytes(Paths.get(pack.getFilePath()));
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Audit pack file not found");
        }
    }
}
