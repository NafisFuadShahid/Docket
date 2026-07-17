package com.compliance.model;

import com.compliance.model.enums.ExtractionMethod;
import com.compliance.model.enums.ExtractionStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "extracted_texts")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ExtractedText {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "document_version_id", nullable = false, unique = true)
    private UUID documentVersionId;

    @Column(name = "full_text", columnDefinition = "text")
    private String fullText;

    @Enumerated(EnumType.STRING)
    @Column(name = "extraction_method")
    @Builder.Default
    private ExtractionMethod extractionMethod = ExtractionMethod.PDF_TEXT;

    @Column(name = "page_count")
    private Integer pageCount;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    @Builder.Default
    private List<Object> chunks = List.of();

    @Enumerated(EnumType.STRING)
    @Column(name = "extraction_status")
    @Builder.Default
    private ExtractionStatus extractionStatus = ExtractionStatus.PENDING;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_version_id", insertable = false, updatable = false)
    private DocumentVersion documentVersion;
}
