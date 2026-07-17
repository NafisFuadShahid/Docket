package com.compliance.service;

import com.compliance.audit.AuditService;
import com.compliance.dto.AuthDtos.*;
import com.compliance.model.User;
import com.compliance.model.enums.UserRole;
import com.compliance.repository.UserRepository;
import com.compliance.security.JwtService;
import com.compliance.security.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditService auditService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       JwtService jwtService, AuditService auditService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.auditService = auditService;
    }

    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
        if (!user.getIsActive()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Account disabled");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getHashedPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
        user.setLastLogin(Instant.now());
        userRepository.save(user);

        TenantContext.setTenantId(user.getTenantId());
        TenantContext.setUserId(user.getId());
        auditService.log("LOGIN", "user", user.getId());

        String access = jwtService.createAccessToken(user.getId(), user.getTenantId(), user.getRole().name());
        String refresh = jwtService.createRefreshToken(user.getId(), user.getTenantId());
        return new TokenResponse(access, refresh);
    }

    public TokenResponse refresh(RefreshRequest request) {
        var claims = jwtService.parseToken(request.getRefreshToken());
        if (!"refresh".equals(claims.get("type"))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
        }
        UUID userId = UUID.fromString(claims.getSubject());
        UUID tenantId = UUID.fromString(claims.get("tenant_id", String.class));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
        String access = jwtService.createAccessToken(userId, tenantId, user.getRole().name());
        String refresh = jwtService.createRefreshToken(userId, tenantId);
        return new TokenResponse(access, refresh);
    }

    public UserResponse getCurrentUser() {
        UUID userId = TenantContext.getUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return toResponse(user);
    }

    public void changePassword(ChangePasswordRequest request) {
        UUID userId = TenantContext.getUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getHashedPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is incorrect");
        }
        user.setHashedPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        auditService.log("CHANGE_PASSWORD", "user", userId);
    }

    public List<UserResponse> listUsers() {
        UUID tenantId = TenantContext.getTenantId();
        return userRepository.findByTenantId(tenantId).stream().map(this::toResponse).toList();
    }

    public UserResponse createUser(CreateUserRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }
        User user = User.builder()
                .tenantId(tenantId)
                .email(request.getEmail())
                .hashedPassword(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .role(UserRole.valueOf(request.getRole()))
                .department(request.getDepartment())
                .build();
        userRepository.save(user);
        auditService.log("CREATE_USER", "user", user.getId(), null,
                Map.of("email", user.getEmail(), "role", user.getRole().name()));
        return toResponse(user);
    }

    public UserResponse updateUser(UUID userId, UpdateUserRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        User user = userRepository.findByIdAndTenantId(userId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Map<String, Object> oldValues = Map.of("role", user.getRole().name(), "department", String.valueOf(user.getDepartment()));
        if (request.getFullName() != null) user.setFullName(request.getFullName());
        if (request.getRole() != null) user.setRole(UserRole.valueOf(request.getRole()));
        if (request.getDepartment() != null) user.setDepartment(request.getDepartment());
        if (request.getIsActive() != null) user.setIsActive(request.getIsActive());
        userRepository.save(user);
        auditService.log("UPDATE_USER", "user", userId, oldValues,
                Map.of("role", user.getRole().name(), "department", String.valueOf(user.getDepartment())));
        return toResponse(user);
    }

    public UserResponse toResponse(User user) {
        UserResponse r = new UserResponse();
        r.setId(user.getId());
        r.setTenantId(user.getTenantId());
        r.setEmail(user.getEmail());
        r.setFullName(user.getFullName());
        r.setRole(user.getRole().name());
        r.setDepartment(user.getDepartment());
        r.setIsActive(user.getIsActive());
        r.setCreatedAt(user.getCreatedAt());
        r.setLastLogin(user.getLastLogin());
        return r;
    }
}
