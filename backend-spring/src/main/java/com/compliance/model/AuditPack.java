package com.compliance.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "audit_packs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuditPack {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "circular_id")
    private UUID circularId;

    @Column(name = "generated_by", nullable = false)
    private UUID generatedBy;

    @Column(nullable = false, length = 500)
    @Builder.Default
    private String title = "Audit Pack";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "pack_data", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> packData = Map.of();

    @Column(name = "file_path", length = 500)
    private String filePath;

    @Column(nullable = false, length = 10)
    @Builder.Default
    private String format = "html";

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
