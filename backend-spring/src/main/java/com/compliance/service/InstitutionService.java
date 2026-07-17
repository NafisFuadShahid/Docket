package com.compliance.service;

import com.compliance.audit.AuditService;
import com.compliance.dto.InstitutionDtos.*;
import com.compliance.model.InstitutionProfile;
import com.compliance.repository.InstitutionProfileRepository;
import com.compliance.security.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.UUID;

@Service
public class InstitutionService {

    private final InstitutionProfileRepository profileRepository;
    private final AuditService auditService;

    public InstitutionService(InstitutionProfileRepository profileRepository, AuditService auditService) {
        this.profileRepository = profileRepository;
        this.auditService = auditService;
    }

    public ProfileResponse getProfile() {
        UUID tenantId = TenantContext.getTenantId();
        InstitutionProfile p = profileRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Institution profile not configured"));
        return toResponse(p);
    }

    public ProfileResponse updateProfile(UpdateProfileRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        InstitutionProfile p = profileRepository.findByTenantId(tenantId)
                .orElse(InstitutionProfile.builder().tenantId(tenantId).build());

        p.setInstitutionType(request.getInstitutionType());
        if (request.getBusinessLines() != null) p.setBusinessLines(request.getBusinessLines());
        if (request.getDepartments() != null) p.setDepartments(request.getDepartments());
        if (request.getRegulators() != null) p.setRegulators(request.getRegulators());
        profileRepository.save(p);

        auditService.log("UPDATE_INSTITUTION_PROFILE", "institution_profile", p.getId());
        return toResponse(p);
    }

    private ProfileResponse toResponse(InstitutionProfile p) {
        ProfileResponse r = new ProfileResponse();
        r.setId(p.getId());
        r.setTenantId(p.getTenantId());
        r.setInstitutionType(p.getInstitutionType());
        r.setBusinessLines(p.getBusinessLines());
        r.setDepartments(p.getDepartments());
        r.setRegulators(p.getRegulators());
        r.setCreatedAt(p.getCreatedAt());
        r.setUpdatedAt(p.getUpdatedAt());
        return r;
    }
}
