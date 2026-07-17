package com.compliance.dto;

import lombok.Data;

import java.time.Instant;
import java.util.UUID;

public class EvidenceDtos {

    @Data
    public static class EvidenceResponse {
        private UUID id;
        private UUID tenantId;
        private UUID taskId;
        private UUID obligationId;
        private String fileName;
        private String contentType;
        private Integer fileSize;
        private String sha256Hash;
        private String evidenceType;
        private UUID uploadedBy;
        private String uploaderName;
        private String description;
        private Instant createdAt;
    }

    @Data
    public static class UploadRequest {
        private UUID taskId;
        private UUID obligationId;
        private String evidenceType;
        private String description;
    }
}
