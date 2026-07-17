package com.compliance.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

public class AuthDtos {

    @Data
    public static class LoginRequest {
        @NotBlank @Email
        private String email;
        @NotBlank
        private String password;
    }

    @Data
    public static class RefreshRequest {
        @NotBlank
        private String refreshToken;
    }

    @Data
    public static class ChangePasswordRequest {
        @NotBlank
        private String currentPassword;
        @NotBlank
        private String newPassword;
    }

    @Data
    public static class TokenResponse {
        private String accessToken;
        private String refreshToken;
        private String tokenType = "Bearer";

        public TokenResponse(String accessToken, String refreshToken) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
        }
    }

    @Data
    public static class UserResponse {
        private UUID id;
        private UUID tenantId;
        private String email;
        private String fullName;
        private String role;
        private String department;
        private Boolean isActive;
        private Instant createdAt;
        private Instant lastLogin;
    }

    @Data
    public static class CreateUserRequest {
        @NotBlank @Email
        private String email;
        @NotBlank
        private String password;
        @NotBlank
        private String fullName;
        @NotBlank
        private String role;
        private String department;
    }

    @Data
    public static class UpdateUserRequest {
        private String fullName;
        private String role;
        private String department;
        private Boolean isActive;
    }
}
