package com.compliance.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class InstitutionDtos {

    @Data
    public static class ProfileResponse {
        private UUID id;
        private UUID tenantId;
        private String institutionType;
        private List<String> businessLines;
        private List<String> departments;
        private List<String> regulators;
        private Instant createdAt;
        private Instant updatedAt;
    }

    @Data
    public static class UpdateProfileRequest {
        @NotBlank
        private String institutionType;
        private List<String> businessLines;
        private List<String> departments;
        private List<String> regulators;
    }
}
