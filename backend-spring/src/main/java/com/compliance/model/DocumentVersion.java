package com.compliance.model;

import com.compliance.model.enums.Language;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "document_versions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DocumentVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "circular_id", nullable = false)
    private UUID circularId;

    @Column(name = "version_number", nullable = false)
    @Builder.Default
    private Integer versionNumber = 1;

    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "content_type")
    @Builder.Default
    private String contentType = "application/pdf";

    @Column(name = "file_size")
    private Integer fileSize;

    @Column(name = "sha256_hash", nullable = false, length = 64)
    private String sha256Hash;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Language language = Language.EN;

    @Column(name = "download_url", length = 1000)
    private String downloadUrl;

    @Column(name = "downloaded_at", nullable = false)
    @Builder.Default
    private Instant downloadedAt = Instant.now();

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "circular_id", insertable = false, updatable = false)
    private Circular circular;
}
