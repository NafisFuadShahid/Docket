package com.compliance.model;

import com.compliance.model.enums.EvidenceType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "evidence_files")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EvidenceFile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "task_id")
    private UUID taskId;

    @Column(name = "obligation_id")
    private UUID obligationId;

    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "file_size", nullable = false)
    private Integer fileSize;

    @Column(name = "sha256_hash", nullable = false, length = 64)
    private String sha256Hash;

    @Enumerated(EnumType.STRING)
    @Column(name = "evidence_type", nullable = false)
    private EvidenceType evidenceType;

    @Column(name = "uploaded_by", nullable = false)
    private UUID uploadedBy;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    @Column(name = "deleted_by")
    private UUID deletedBy;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
