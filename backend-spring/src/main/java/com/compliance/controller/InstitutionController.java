package com.compliance.controller;

import com.compliance.dto.InstitutionDtos.*;
import com.compliance.service.InstitutionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/institution-profile")
public class InstitutionController {

    private final InstitutionService institutionService;

    public InstitutionController(InstitutionService institutionService) {
        this.institutionService = institutionService;
    }

    @GetMapping
    public ResponseEntity<ProfileResponse> get() {
        return ResponseEntity.ok(institutionService.getProfile());
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','COMPLIANCE_ADMIN')")
    public ResponseEntity<ProfileResponse> update(@Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(institutionService.updateProfile(request));
    }
}
