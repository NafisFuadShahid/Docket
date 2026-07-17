package com.compliance.model;

import com.compliance.model.enums.CircularStatus;
import com.compliance.model.enums.Language;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "circulars")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Circular {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "source_id", nullable = false)
    private UUID sourceId;

    @Column(name = "circular_number")
    private String circularNumber;

    @Column(nullable = false, columnDefinition = "text")
    private String title;

    @Column(name = "title_bn", columnDefinition = "text")
    private String titleBn;

    private String department;

    @Column(name = "issued_date")
    private Instant issuedDate;

    @Column(name = "effective_date")
    private Instant effectiveDate;

    @Column(name = "source_url", length = 1000)
    private String sourceUrl;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Language language = Language.EN;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private CircularStatus status = CircularStatus.DETECTED;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_metadata", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> rawMetadata = Map.of();

    @Column(name = "first_seen_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant firstSeenAt = Instant.now();

    @Column(name = "last_seen_at", nullable = false)
    @Builder.Default
    private Instant lastSeenAt = Instant.now();

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_id", insertable = false, updatable = false)
    private RegulatorySource source;

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
