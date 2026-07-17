package com.compliance.dto;

import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CircularDtos {

    @Data
    public static class CircularResponse {
        private UUID id;
        private UUID sourceId;
        private String sourceName;
        private String sourceType;
        private String circularNumber;
        private String title;
        private String titleBn;
        private String department;
        private Instant issuedDate;
        private Instant effectiveDate;
        private String sourceUrl;
        private String language;
        private String status;
        private Map<String, Object> rawMetadata;
        private Instant firstSeenAt;
        private Instant lastSeenAt;
        private Instant createdAt;
        private int versionCount;
    }

    @Data
    public static class CircularDetailResponse {
        private CircularResponse circular;
        private List<DocumentVersionResponse> versions;
        private String extractedText;
        private List<ObligationDtos.ObligationResponse> obligations;
    }

    @Data
    public static class DocumentVersionResponse {
        private UUID id;
        private int versionNumber;
        private String fileName;
        private String contentType;
        private Integer fileSize;
        private String sha256Hash;
        private String language;
        private Instant downloadedAt;
    }

    @Data
    public static class ManualUploadRequest {
        private String title;
        private String circularNumber;
        private String department;
        private String sourceType;
        private String language;
    }

    @Data
    public static class CrawlResultCallback {
        private UUID sourceId;
        private List<ParsedCircular> circulars;
    }

    @Data
    public static class ParsedCircular {
        private String circularNumber;
        private String title;
        private String titleBn;
        private String department;
        private String issuedDate;
        private String sourceUrl;
        private String language;
        private String pdfUrl;
        private String pdfUrlBn;
        private Map<String, Object> metadata;
    }

    @Data
    public static class ExtractionResultCallback {
        private UUID documentVersionId;
        private String fullText;
        private String extractionMethod;
        private int pageCount;
        private List<Object> chunks;
        private String status;
        private String errorMessage;
    }
}
