package com.compliance.model;

import com.compliance.model.enums.ApplicabilityStatus;
import com.compliance.model.enums.ReviewStatus;
import com.compliance.model.enums.Severity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "obligations")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Obligation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "circular_id", nullable = false)
    private UUID circularId;

    @Column(name = "obligation_title", nullable = false, length = 500)
    private String obligationTitle;

    @Column(name = "obligation_detail", nullable = false, columnDefinition = "text")
    private String obligationDetail;

    @Column(name = "source_quote", columnDefinition = "text")
    private String sourceQuote;

    @Column(name = "source_page")
    private Integer sourcePage;

    @Column(nullable = false, length = 100)
    private String regulator;

    @Column(name = "circular_number", length = 100)
    private String circularNumber;

    @Column(name = "source_department")
    private String sourceDepartment;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "affected_institution_types", columnDefinition = "jsonb")
    @Builder.Default
    private List<String> affectedInstitutionTypes = List.of();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "affected_business_lines", columnDefinition = "jsonb")
    @Builder.Default
    private List<String> affectedBusinessLines = List.of();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "impacted_departments", columnDefinition = "jsonb")
    @Builder.Default
    private List<String> impactedDepartments = List.of();

    private Instant deadline;

    @Column(name = "effective_date")
    private Instant effectiveDate;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "required_actions", columnDefinition = "jsonb")
    @Builder.Default
    private List<String> requiredActions = List.of();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "required_evidence", columnDefinition = "jsonb")
    @Builder.Default
    private List<String> requiredEvidence = List.of();

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Severity severity = Severity.MEDIUM;

    @Builder.Default
    private Double confidence = 0.0;

    @Column(columnDefinition = "text")
    private String rationale;

    @Column(name = "ai_model_used", length = 100)
    private String aiModelUsed;

    @Column(name = "extraction_version", length = 50)
    private String extractionVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "review_status", nullable = false)
    @Builder.Default
    private ReviewStatus reviewStatus = ReviewStatus.PENDING;

    @Column(name = "reviewed_by")
    private UUID reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "reviewer_notes", columnDefinition = "text")
    private String reviewerNotes;

    @Enumerated(EnumType.STRING)
    @Column(name = "applicability_status", nullable = false)
    @Builder.Default
    private ApplicabilityStatus applicabilityStatus = ApplicabilityStatus.NEEDS_REVIEW;

    @Column(name = "applicability_reason", columnDefinition = "text")
    private String applicabilityReason;

    @Column(name = "applicability_overridden_by")
    private UUID applicabilityOverriddenBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "circular_id", insertable = false, updatable = false)
    private Circular circular;

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
