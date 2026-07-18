package com.compliance.config;

import com.compliance.model.*;
import com.compliance.model.enums.*;
import com.compliance.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@Profile("!test")
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final TenantRepository tenantRepo;
    private final UserRepository userRepo;
    private final InstitutionProfileRepository profileRepo;
    private final RegulatorySourceRepository sourceRepo;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(TenantRepository tenantRepo, UserRepository userRepo,
                      InstitutionProfileRepository profileRepo, RegulatorySourceRepository sourceRepo,
                      PasswordEncoder passwordEncoder) {
        this.tenantRepo = tenantRepo;
        this.userRepo = userRepo;
        this.profileRepo = profileRepo;
        this.sourceRepo = sourceRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (tenantRepo.count() > 0) {
            log.info("seed: data already exists, skipping");
            return;
        }
        log.info("seed: creating initial data");

        Tenant tenant = tenantRepo.save(Tenant.builder()
                .name("Demo National Bank Ltd")
                .slug("demo-bank")
                .institutionType("scheduled_bank")
                .licenseNumber("BB-SCH-001")
                .settings(Map.of("autoApprove", false))
                .build());

        userRepo.save(User.builder()
                .tenantId(tenant.getId())
                .email("admin@demo-bank.com")
                .hashedPassword(passwordEncoder.encode("admin123"))
                .fullName("Compliance Admin")
                .role(UserRole.COMPLIANCE_ADMIN)
                .department("compliance")
                .build());

        userRepo.save(User.builder()
                .tenantId(tenant.getId())
                .email("reviewer@demo-bank.com")
                .hashedPassword(passwordEncoder.encode("reviewer123"))
                .fullName("Senior Reviewer")
                .role(UserRole.REVIEWER)
                .department("compliance")
                .build());

        userRepo.save(User.builder()
                .tenantId(tenant.getId())
                .email("auditor@demo-bank.com")
                .hashedPassword(passwordEncoder.encode("auditor123"))
                .fullName("Internal Auditor")
                .role(UserRole.AUDITOR)
                .department("internal_audit")
                .build());

        userRepo.save(User.builder()
                .tenantId(tenant.getId())
                .email("treasury@demo-bank.com")
                .hashedPassword(passwordEncoder.encode("treasury123"))
                .fullName("Treasury Head")
                .role(UserRole.DEPARTMENT_OWNER)
                .department("treasury")
                .build());

        userRepo.save(User.builder()
                .tenantId(tenant.getId())
                .email("aml@demo-bank.com")
                .hashedPassword(passwordEncoder.encode("aml123"))
                .fullName("AML/CFT Officer")
                .role(UserRole.DEPARTMENT_OWNER)
                .department("aml_cft")
                .build());

        profileRepo.save(InstitutionProfile.builder()
                .tenantId(tenant.getId())
                .institutionType("scheduled_bank")
                .businessLines(List.of("foreign_exchange", "trade_finance", "treasury", "card_payment", "sme_agri_loans", "branch_network"))
                .departments(List.of("credit_risk", "trade_finance", "aml_cft", "ict_security", "treasury", "operations", "legal", "branch_banking", "compliance"))
                .regulators(List.of("bangladesh_bank", "bfiu"))
                .build());

        sourceRepo.save(RegulatorySource.builder()
                .name("Bangladesh Bank Circulars")
                .slug("bb-circulars")
                .baseUrl("https://www.bb.org.bd/en/index.php/mediaroom/circular")
                .sourceType(SourceType.BB_CIRCULAR)
                .crawlIntervalMinutes(15)
                .build());

        sourceRepo.save(RegulatorySource.builder()
                .name("BFIU Circulars")
                .slug("bfiu-circulars")
                .baseUrl("https://www.bfiu.org.bd/index.php/legislation/circular")
                .sourceType(SourceType.BFIU_CIRCULAR)
                .crawlIntervalMinutes(15)
                .build());

        sourceRepo.save(RegulatorySource.builder()
                .name("Bangladesh Bank Guidelines")
                .slug("bb-guidelines")
                .baseUrl("https://www.bb.org.bd/en/index.php/about/guidelist")
                .sourceType(SourceType.BB_GUIDELINE)
                .crawlIntervalMinutes(30)
                .build());

        sourceRepo.save(RegulatorySource.builder()
                .name("Bangladesh Bank Notices")
                .slug("bb-notices")
                .baseUrl("https://www.bb.org.bd/en/index.php/mediaroom/noticeboard")
                .sourceType(SourceType.BB_NOTICE)
                .crawlIntervalMinutes(30)
                .build());

        log.info("seed: initial data created — tenant={} users=5 sources=4", tenant.getSlug());
    }
}
