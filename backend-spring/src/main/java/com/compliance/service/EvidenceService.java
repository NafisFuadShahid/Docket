package com.compliance.service;

import com.compliance.audit.AuditService;
import com.compliance.dto.EvidenceDtos.*;
import com.compliance.model.EvidenceFile;
import com.compliance.model.enums.EvidenceType;
import com.compliance.repository.EvidenceFileRepository;
import com.compliance.repository.UserRepository;
import com.compliance.security.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class EvidenceService {

    private static final Logger log = LoggerFactory.getLogger(EvidenceService.class);

    private final EvidenceFileRepository evidenceRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    @Value("${app.storage.local-path}")
    private String storagePath;

    public EvidenceService(EvidenceFileRepository evidenceRepository,
                           UserRepository userRepository, AuditService auditService) {
        this.evidenceRepository = evidenceRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    public List<EvidenceResponse> listEvidence() {
        UUID tenantId = TenantContext.getTenantId();
        return evidenceRepository.findByTenantIdAndIsDeletedFalse(tenantId)
                .stream().map(this::toResponse).toList();
    }

    public List<EvidenceResponse> listByTask(UUID taskId) {
        return evidenceRepository.findByTaskIdAndIsDeletedFalse(taskId)
                .stream().map(this::toResponse).toList();
    }

    public List<EvidenceResponse> listByObligation(UUID obligationId) {
        return evidenceRepository.findByObligationIdAndIsDeletedFalse(obligationId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public EvidenceResponse upload(MultipartFile file, UploadRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        UUID userId = TenantContext.getUserId();

        if (file.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is empty");
        if (file.getSize() > 50 * 1024 * 1024) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File too large");

        try {
            byte[] content = file.getBytes();
            String hash = sha256(content);
            String dir = storagePath + "/evidence/" + tenantId;
            Files.createDirectories(Paths.get(dir));
            String filePath = dir + "/" + UUID.randomUUID() + "_" + file.getOriginalFilename();
            Files.write(Paths.get(filePath), content);

            EvidenceFile ef = EvidenceFile.builder()
                    .tenantId(tenantId)
                    .taskId(request.getTaskId())
                    .obligationId(request.getObligationId())
                    .filePath(filePath)
                    .fileName(file.getOriginalFilename())
                    .contentType(file.getContentType())
                    .fileSize((int) file.getSize())
                    .sha256Hash(hash)
                    .evidenceType(EvidenceType.valueOf(request.getEvidenceType()))
                    .uploadedBy(userId)
                    .description(request.getDescription())
                    .build();
            evidenceRepository.save(ef);

            auditService.log("UPLOAD_EVIDENCE", "evidence", ef.getId(), null,
                    Map.of("fileName", ef.getFileName(), "hash", hash));
            log.info("evidence_uploaded id={} file={} hash={}", ef.getId(), ef.getFileName(), hash);
            return toResponse(ef);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "File storage failed");
        }
    }

    public byte[] download(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        EvidenceFile ef = evidenceRepository.findByIdAndTenantIdAndIsDeletedFalse(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        try {
            return Files.readAllBytes(Paths.get(ef.getFilePath()));
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found on disk");
        }
    }

    @Transactional
    public void softDelete(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        UUID userId = TenantContext.getUserId();
        EvidenceFile ef = evidenceRepository.findByIdAndTenantIdAndIsDeletedFalse(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        ef.setIsDeleted(true);
        ef.setDeletedBy(userId);
        ef.setDeletedAt(Instant.now());
        evidenceRepository.save(ef);
        auditService.log("DELETE_EVIDENCE", "evidence", id, Map.of("fileName", ef.getFileName()), null);
    }

    private String sha256(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(data));
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    private EvidenceResponse toResponse(EvidenceFile ef) {
        EvidenceResponse r = new EvidenceResponse();
        r.setId(ef.getId());
        r.setTenantId(ef.getTenantId());
        r.setTaskId(ef.getTaskId());
        r.setObligationId(ef.getObligationId());
        r.setFileName(ef.getFileName());
        r.setContentType(ef.getContentType());
        r.setFileSize(ef.getFileSize());
        r.setSha256Hash(ef.getSha256Hash());
        r.setEvidenceType(ef.getEvidenceType().name());
        r.setUploadedBy(ef.getUploadedBy());
        r.setDescription(ef.getDescription());
        r.setCreatedAt(ef.getCreatedAt());
        userRepository.findById(ef.getUploadedBy()).ifPresent(u -> r.setUploaderName(u.getFullName()));
        return r;
    }
}
