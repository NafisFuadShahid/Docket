package com.compliance;

import com.compliance.model.Tenant;
import com.compliance.model.User;
import com.compliance.model.enums.UserRole;
import com.compliance.repository.TenantRepository;
import com.compliance.repository.UserRepository;
import com.compliance.security.JwtService;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class AuthServiceTest {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TenantRepository tenantRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtService jwtService;

    private Tenant tenant;

    @BeforeEach
    void setup() {
        userRepository.deleteAll();
        tenantRepository.deleteAll();
        tenant = new Tenant();
        tenant.setName("Test Bank");
        tenant.setSlug("test-bank");
        tenant = tenantRepository.save(tenant);
    }

    @Test
    void jwtRoundTrip() {
        User user = User.builder()
                .tenantId(tenant.getId())
                .email("test@example.com")
                .hashedPassword(passwordEncoder.encode("password"))
                .fullName("Test User")
                .role(UserRole.SUPER_ADMIN)
                .isActive(true)
                .build();
        user = userRepository.save(user);

        String token = jwtService.createAccessToken(
                user.getId(),
                tenant.getId(),
                user.getRole().name()
        );

        assertNotNull(token);
        assertTrue(jwtService.isValid(token));

        Claims claims = jwtService.parseToken(token);
        assertEquals(user.getId().toString(), claims.getSubject());
        assertEquals(tenant.getId().toString(), claims.get("tenant_id"));
        assertEquals("SUPER_ADMIN", claims.get("role"));
    }

    @Test
    void passwordEncoding() {
        String raw = "securePassword123";
        String encoded = passwordEncoder.encode(raw);
        assertTrue(passwordEncoder.matches(raw, encoded));
        assertFalse(passwordEncoder.matches("wrong", encoded));
    }
}
