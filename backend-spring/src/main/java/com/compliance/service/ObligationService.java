package com.compliance.service;

import com.compliance.audit.AuditService;
import com.compliance.dto.ObligationDtos.*;
import com.compliance.model.Circular;
import com.compliance.model.Obligation;
import com.compliance.model.enums.*;
import com.compliance.repository.CircularRepository;
import com.compliance.repository.ObligationRepository;
import com.compliance.security.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ObligationService {

    private static final Logger log = LoggerFactory.getLogger(ObligationService.class);

    private final ObligationRepository obligationRepository;
    private final CircularRepository circularRepository;
    private final AuditService auditService;

    public ObligationService(ObligationRepository obligationRepository,
                             CircularRepository circularRepository,
                             AuditService auditService) {
        this.obligationRepository = obligationRepository;
        this.circularRepository = circularRepository;
        this.auditService = auditService;
    }

    public Page<ObligationResponse> listObligations(UUID tenantId, Pageable pageable) {
        return obligationRepository.findByTenantId(tenantId, pageable).map(this::toResponse);
    }

    public ObligationResponse getObligation(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        Obligation o = obligationRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return toResponse(o);
    }

    public List<ObligationResponse> getPendingReview() {
        UUID tenantId = TenantContext.getTenantId();
        return obligationRepository.findByTenantIdAndReviewStatus(tenantId, ReviewStatus.PENDING)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public ObligationResponse reviewObligation(UUID id, ReviewRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        UUID userId = TenantContext.getUserId();
        Obligation o = obligationRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        Map<String, Object> oldValues = Map.of("reviewStatus", o.getReviewStatus().name());

        switch (request.getAction().toLowerCase()) {
            case "approve" -> o.setReviewStatus(ReviewStatus.APPROVED);
            case "reject" -> o.setReviewStatus(ReviewStatus.REJECTED);
            case "edit" -> {
                o.setReviewStatus(ReviewStatus.EDITED);
                if (request.getObligationTitle() != null) o.setObligationTitle(request.getObligationTitle());
                if (request.getObligationDetail() != null) o.setObligationDetail(request.getObligationDetail());
                if (request.getSeverity() != null) o.setSeverity(Severity.valueOf(request.getSeverity().toUpperCase()));
                if (request.getImpactedDepartments() != null) o.setImpactedDepartments(request.getImpactedDepartments());
                if (request.getRequiredActions() != null) o.setRequiredActions(request.getRequiredActions());
            }
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid action");
        }

        o.setReviewedBy(userId);
        o.setReviewedAt(Instant.now());
        o.setReviewerNotes(request.getReviewerNotes());
        obligationRepository.save(o);

        auditService.log("REVIEW_OBLIGATION", "obligation", id, oldValues,
                Map.of("reviewStatus", o.getReviewStatus().name(), "action", request.getAction()));

        Circular circular = circularRepository.findById(o.getCircularId()).orElse(null);
        if (circular != null && circular.getStatus() == CircularStatus.AI_PROCESSED) {
            circular.setStatus(CircularStatus.PENDING_REVIEW);
            circularRepository.save(circular);
        }

        log.info("obligation_reviewed id={} action={} reviewer={}", id, request.getAction(), userId);
        return toResponse(o);
    }

    @Transactional
    public ObligationResponse overrideApplicability(UUID id, ApplicabilityOverrideRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        UUID userId = TenantContext.getUserId();
        Obligation o = obligationRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        Map<String, Object> oldValues = Map.of("applicabilityStatus", o.getApplicabilityStatus().name());

        o.setApplicabilityStatus(ApplicabilityStatus.valueOf(request.getApplicabilityStatus().toUpperCase()));
        o.setApplicabilityReason(request.getReason());
        o.setApplicabilityOverriddenBy(userId);
        obligationRepository.save(o);

        auditService.log("OVERRIDE_APPLICABILITY", "obligation", id, oldValues,
                Map.of("applicabilityStatus", o.getApplicabilityStatus().name(), "reason", request.getReason()));
        return toResponse(o);
    }

    @Transactional
    public void handleExtractionCallback(ObligationExtractionCallback callback) {
        for (ExtractedObligation eo : callback.getObligations()) {
            Obligation o = Obligation.builder()
                    .tenantId(callback.getTenantId())
                    .circularId(callback.getCircularId())
                    .obligationTitle(eo.getObligationTitle())
                    .obligationDetail(eo.getObligationDetail())
                    .sourceQuote(eo.getSourceQuote())
                    .sourcePage(eo.getSourcePage())
                    .regulator(eo.getRegulator())
                    .circularNumber(eo.getCircularNumber())
                    .sourceDepartment(eo.getSourceDepartment())
                    .affectedInstitutionTypes(eo.getAffectedInstitutionTypes() != null ? eo.getAffectedInstitutionTypes() : List.of())
                    .affectedBusinessLines(eo.getAffectedBusinessLines() != null ? eo.getAffectedBusinessLines() : List.of())
                    .impactedDepartments(eo.getImpactedDepartments() != null ? eo.getImpactedDepartments() : List.of())
                    .requiredActions(eo.getRequiredActions() != null ? eo.getRequiredActions() : List.of())
                    .requiredEvidence(eo.getRequiredEvidence() != null ? eo.getRequiredEvidence() : List.of())
                    .severity(eo.getSeverity() != null ? Severity.valueOf(eo.getSeverity().toUpperCase()) : Severity.MEDIUM)
                    .confidence(eo.getConfidence() != null ? eo.getConfidence() : 0.0)
                    .rationale(eo.getRationale())
                    .aiModelUsed(callback.getModelUsed())
                    .reviewStatus(ReviewStatus.PENDING)
                    .applicabilityStatus(ApplicabilityStatus.NEEDS_REVIEW)
                    .build();
            obligationRepository.save(o);
        }

        Circular circular = circularRepository.findById(callback.getCircularId()).orElse(null);
        if (circular != null) {
            circular.setStatus(CircularStatus.AI_PROCESSED);
            circularRepository.save(circular);
        }
        log.info("obligations_extracted circular={} count={}", callback.getCircularId(), callback.getObligations().size());
    }

    public DashboardStats getDashboardStats() {
        UUID tenantId = TenantContext.getTenantId();
        DashboardStats stats = new DashboardStats();
        stats.setTotalObligations(obligationRepository.findByTenantId(tenantId, Pageable.unpaged()).getTotalElements());
        stats.setPendingReview(obligationRepository.countByTenantIdAndReviewStatus(tenantId, ReviewStatus.PENDING));
        stats.setApproved(obligationRepository.countByTenantIdAndReviewStatus(tenantId, ReviewStatus.APPROVED) +
                          obligationRepository.countByTenantIdAndReviewStatus(tenantId, ReviewStatus.EDITED));
        stats.setRejected(obligationRepository.countByTenantIdAndReviewStatus(tenantId, ReviewStatus.REJECTED));
        stats.setApplicable(obligationRepository.countByTenantIdAndApplicabilityStatus(tenantId, ApplicabilityStatus.APPLICABLE));
        stats.setNotApplicable(obligationRepository.countByTenantIdAndApplicabilityStatus(tenantId, ApplicabilityStatus.NOT_APPLICABLE));
        stats.setHighSeverity(obligationRepository.countByTenantIdAndSeverity(tenantId, Severity.HIGH));
        stats.setCriticalSeverity(obligationRepository.countByTenantIdAndSeverity(tenantId, Severity.CRITICAL));
        return stats;
    }

    public ObligationResponse toResponse(Obligation o) {
        ObligationResponse r = new ObligationResponse();
        r.setId(o.getId());
        r.setTenantId(o.getTenantId());
        r.setCircularId(o.getCircularId());
        r.setObligationTitle(o.getObligationTitle());
        r.setObligationDetail(o.getObligationDetail());
        r.setSourceQuote(o.getSourceQuote());
        r.setSourcePage(o.getSourcePage());
        r.setRegulator(o.getRegulator());
        r.setCircularNumber(o.getCircularNumber());
        r.setSourceDepartment(o.getSourceDepartment());
        r.setAffectedInstitutionTypes(o.getAffectedInstitutionTypes());
        r.setAffectedBusinessLines(o.getAffectedBusinessLines());
        r.setImpactedDepartments(o.getImpactedDepartments());
        r.setDeadline(o.getDeadline());
        r.setEffectiveDate(o.getEffectiveDate());
        r.setRequiredActions(o.getRequiredActions());
        r.setRequiredEvidence(o.getRequiredEvidence());
        r.setSeverity(o.getSeverity().name());
        r.setConfidence(o.getConfidence());
        r.setRationale(o.getRationale());
        r.setAiModelUsed(o.getAiModelUsed());
        r.setReviewStatus(o.getReviewStatus().name());
        r.setReviewedBy(o.getReviewedBy());
        r.setReviewedAt(o.getReviewedAt());
        r.setReviewerNotes(o.getReviewerNotes());
        r.setApplicabilityStatus(o.getApplicabilityStatus().name());
        r.setApplicabilityReason(o.getApplicabilityReason());
        r.setCreatedAt(o.getCreatedAt());

        circularRepository.findById(o.getCircularId())
                .ifPresent(c -> r.setCircularTitle(c.getTitle()));
        return r;
    }
}
