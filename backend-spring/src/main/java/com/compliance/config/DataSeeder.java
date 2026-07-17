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

import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
    private final CircularRepository circularRepo;
    private final DocumentVersionRepository docVersionRepo;
    private final ExtractedTextRepository extractedTextRepo;
    private final ObligationRepository obligationRepo;
    private final TaskRepository taskRepo;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(TenantRepository tenantRepo, UserRepository userRepo,
                      InstitutionProfileRepository profileRepo, RegulatorySourceRepository sourceRepo,
                      CircularRepository circularRepo, DocumentVersionRepository docVersionRepo,
                      ExtractedTextRepository extractedTextRepo, ObligationRepository obligationRepo,
                      TaskRepository taskRepo, PasswordEncoder passwordEncoder) {
        this.tenantRepo = tenantRepo;
        this.userRepo = userRepo;
        this.profileRepo = profileRepo;
        this.sourceRepo = sourceRepo;
        this.circularRepo = circularRepo;
        this.docVersionRepo = docVersionRepo;
        this.extractedTextRepo = extractedTextRepo;
        this.obligationRepo = obligationRepo;
        this.taskRepo = taskRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (tenantRepo.count() > 0) {
            log.info("seed: data already exists, skipping");
            return;
        }
        log.info("seed: creating demo data");

        Tenant tenant = tenantRepo.save(Tenant.builder()
                .name("Demo National Bank Ltd")
                .slug("demo-bank")
                .institutionType("scheduled_bank")
                .licenseNumber("BB-SCH-001")
                .settings(Map.of("autoApprove", false))
                .build());

        var admin = userRepo.save(User.builder()
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

        var bbCirculars = sourceRepo.save(RegulatorySource.builder()
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

        var circular1 = circularRepo.save(Circular.builder()
                .sourceId(bbCirculars.getId())
                .circularNumber("BRPD Circular No. 15")
                .title("Revised Policy on Loan Classification and Provisioning")
                .department("Banking Regulation & Policy Department")
                .issuedDate(Instant.now().minus(30, ChronoUnit.DAYS))
                .sourceUrl("https://www.bb.org.bd/en/index.php/mediaroom/circular")
                .language(Language.BOTH)
                .status(CircularStatus.AI_PROCESSED)
                .build());

        var circular2 = circularRepo.save(Circular.builder()
                .sourceId(bbCirculars.getId())
                .circularNumber("FEPD Circular No. 08")
                .title("Guidelines on Foreign Exchange Transactions for Authorized Dealers")
                .department("Foreign Exchange Policy Department")
                .issuedDate(Instant.now().minus(15, ChronoUnit.DAYS))
                .sourceUrl("https://www.bb.org.bd/en/index.php/mediaroom/circular")
                .language(Language.EN)
                .status(CircularStatus.PENDING_REVIEW)
                .build());

        docVersionRepo.save(DocumentVersion.builder()
                .circularId(circular1.getId())
                .versionNumber(1)
                .filePath("fixtures/sample-circular-1.pdf")
                .fileName("brpd-circular-15.pdf")
                .sha256Hash("a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2")
                .language(Language.EN)
                .fileSize(245000)
                .build());

        var ob1 = obligationRepo.save(Obligation.builder()
                .tenantId(tenant.getId())
                .circularId(circular1.getId())
                .obligationTitle("Update Loan Classification Criteria")
                .obligationDetail("All scheduled banks must update their loan classification criteria per the revised policy. Loans overdue by 3 months must be classified as sub-standard.")
                .sourceQuote("Banks shall classify loans overdue by 3 (three) months as Sub-Standard from the quarter ending...")
                .sourcePage(2)
                .regulator("Bangladesh Bank")
                .circularNumber("BRPD Circular No. 15")
                .sourceDepartment("Banking Regulation & Policy Department")
                .affectedInstitutionTypes(List.of("scheduled_bank"))
                .affectedBusinessLines(List.of("sme_agri_loans"))
                .impactedDepartments(List.of("credit_risk", "operations"))
                .deadline(Instant.now().plus(60, ChronoUnit.DAYS))
                .requiredActions(List.of("Update policy document", "Configure core banking system", "Train credit officers", "Issue internal memo"))
                .requiredEvidence(List.of("Updated policy", "System configuration screenshot", "Training attendance", "Internal memo copy"))
                .severity(Severity.HIGH)
                .confidence(0.92)
                .rationale("Directly mandates changes to loan classification. Non-compliance risks regulatory action.")
                .aiModelUsed("mock-provider")
                .reviewStatus(ReviewStatus.APPROVED)
                .reviewedBy(admin.getId())
                .reviewedAt(Instant.now().minus(5, ChronoUnit.DAYS))
                .applicabilityStatus(ApplicabilityStatus.APPLICABLE)
                .applicabilityReason("Demo National Bank is a scheduled bank with active SME/agri loan portfolio.")
                .build());

        obligationRepo.save(Obligation.builder()
                .tenantId(tenant.getId())
                .circularId(circular1.getId())
                .obligationTitle("Submit Quarterly Provisioning Report")
                .obligationDetail("Submit revised provisioning adequacy report to BRPD within 45 days of quarter end.")
                .sourceQuote("Banks shall submit provisioning adequacy report in the prescribed format...")
                .sourcePage(5)
                .regulator("Bangladesh Bank")
                .circularNumber("BRPD Circular No. 15")
                .sourceDepartment("Banking Regulation & Policy Department")
                .affectedInstitutionTypes(List.of("scheduled_bank"))
                .impactedDepartments(List.of("credit_risk", "compliance"))
                .deadline(Instant.now().plus(45, ChronoUnit.DAYS))
                .requiredActions(List.of("Prepare provisioning report", "Submit to Bangladesh Bank"))
                .severity(Severity.CRITICAL)
                .confidence(0.95)
                .aiModelUsed("mock-provider")
                .reviewStatus(ReviewStatus.PENDING)
                .applicabilityStatus(ApplicabilityStatus.NEEDS_REVIEW)
                .build());

        obligationRepo.save(Obligation.builder()
                .tenantId(tenant.getId())
                .circularId(circular2.getId())
                .obligationTitle("Update FX Transaction Reporting")
                .obligationDetail("Authorized Dealer branches must implement revised FX transaction reporting format effective immediately.")
                .regulator("Bangladesh Bank")
                .circularNumber("FEPD Circular No. 08")
                .sourceDepartment("Foreign Exchange Policy Department")
                .affectedInstitutionTypes(List.of("scheduled_bank"))
                .affectedBusinessLines(List.of("foreign_exchange"))
                .impactedDepartments(List.of("treasury", "trade_finance"))
                .requiredActions(List.of("Update reporting format", "Configure treasury system", "Train AD branch staff"))
                .severity(Severity.MEDIUM)
                .confidence(0.88)
                .aiModelUsed("mock-provider")
                .reviewStatus(ReviewStatus.PENDING)
                .applicabilityStatus(ApplicabilityStatus.NEEDS_REVIEW)
                .build());

        taskRepo.save(Task.builder()
                .tenantId(tenant.getId())
                .obligationId(ob1.getId())
                .circularId(circular1.getId())
                .title("Update loan classification policy document")
                .description("Revise the internal policy document to align with BRPD Circular No. 15")
                .taskType(TaskType.UPDATE_POLICY)
                .department("credit_risk")
                .dueDate(Instant.now().plus(30, ChronoUnit.DAYS))
                .priority(Priority.HIGH)
                .evidenceRequired(true)
                .build());

        taskRepo.save(Task.builder()
                .tenantId(tenant.getId())
                .obligationId(ob1.getId())
                .circularId(circular1.getId())
                .title("Configure core banking system for new classification rules")
                .taskType(TaskType.CONFIGURE_SYSTEM)
                .department("operations")
                .dueDate(Instant.now().plus(45, ChronoUnit.DAYS))
                .priority(Priority.HIGH)
                .evidenceRequired(true)
                .build());

        taskRepo.save(Task.builder()
                .tenantId(tenant.getId())
                .obligationId(ob1.getId())
                .circularId(circular1.getId())
                .title("Train credit officers on revised classification criteria")
                .taskType(TaskType.TRAIN_STAFF)
                .department("credit_risk")
                .dueDate(Instant.now().plus(20, ChronoUnit.DAYS))
                .priority(Priority.MEDIUM)
                .evidenceRequired(true)
                .build());

        taskRepo.save(Task.builder()
                .tenantId(tenant.getId())
                .obligationId(ob1.getId())
                .circularId(circular1.getId())
                .title("Issue internal memo on classification changes")
                .taskType(TaskType.ISSUE_MEMO)
                .department("compliance")
                .dueDate(Instant.now().plus(7, ChronoUnit.DAYS))
                .priority(Priority.MEDIUM)
                .status(TaskStatus.COMPLETED)
                .evidenceRequired(true)
                .build());

        log.info("seed: demo data created — tenant={} users=5 sources=4 circulars=2 obligations=3 tasks=4", tenant.getSlug());
    }
}
