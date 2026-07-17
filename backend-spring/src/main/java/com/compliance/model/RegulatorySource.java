package com.compliance.model;

import com.compliance.model.enums.SourceType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "regulatory_sources")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RegulatorySource {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(name = "base_url", nullable = false)
    private String baseUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false)
    private SourceType sourceType;

    @Column(name = "crawl_interval_minutes")
    @Builder.Default
    private Integer crawlIntervalMinutes = 15;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "last_crawled_at")
    private Instant lastCrawledAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
