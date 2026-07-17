package com.compliance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class ObligationDtos {

    @Data
    public static class ObligationResponse {
        private UUID id;
        private UUID tenantId;
        private UUID circularId;
        private String circularTitle;
        private String obligationTitle;
        private String obligationDetail;
        private String sourceQuote;
        private Integer sourcePage;
        private String regulator;
        private String circularNumber;
        private String sourceDepartment;
        private List<String> affectedInstitutionTypes;
        private List<String> affectedBusinessLines;
        private List<String> impactedDepartments;
        private Instant deadline;
        private Instant effectiveDate;
        private List<String> requiredActions;
        private List<String> requiredEvidence;
        private String severity;
        private Double confidence;
        private String rationale;
        private String aiModelUsed;
        private String reviewStatus;
        private UUID reviewedBy;
        private Instant reviewedAt;
        private String reviewerNotes;
        private String applicabilityStatus;
        private String applicabilityReason;
        private Instant createdAt;
    }

    @Data
    public static class ReviewRequest {
        @NotBlank
        private String action; // approve, reject, edit
        private String reviewerNotes;
        private String obligationTitle;
        private String obligationDetail;
        private String severity;
        private List<String> impactedDepartments;
        private List<String> requiredActions;
    }

    @Data
    public static class ApplicabilityOverrideRequest {
        @NotBlank
        private String applicabilityStatus;
        @NotBlank
        private String reason;
    }

    @Data
    public static class ObligationExtractionCallback {
        private UUID circularId;
        private UUID tenantId;
        private List<ExtractedObligation> obligations;
        private String modelUsed;
    }

    @Data
    public static class ExtractedObligation {
        private String obligationTitle;
        private String obligationDetail;
        private String sourceQuote;
        private Integer sourcePage;
        private String regulator;
        private String circularNumber;
        private String sourceDepartment;
        private List<String> affectedInstitutionTypes;
        private List<String> affectedBusinessLines;
        private List<String> impactedDepartments;
        private String deadline;
        private String effectiveDate;
        private List<String> requiredActions;
        private List<String> requiredEvidence;
        private String severity;
        private Double confidence;
        private String rationale;
    }

    @Data
    public static class DashboardStats {
        private long totalObligations;
        private long pendingReview;
        private long approved;
        private long rejected;
        private long applicable;
        private long notApplicable;
        private long highSeverity;
        private long criticalSeverity;
    }
}
