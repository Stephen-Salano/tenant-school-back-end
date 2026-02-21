package io.psyllabus.platform.school.service;

import io.psyllabus.platform.auth.dto.request.RegisterRequest;
import io.psyllabus.platform.auth.enums.AccessScope;
import io.psyllabus.platform.auth.enums.UserType;
import io.psyllabus.platform.auth.services.AuthService;
import io.psyllabus.platform.branch.entities.Branch;
import io.psyllabus.platform.branch.repository.BranchRepository;
import io.psyllabus.platform.common.constants.FeatureConstants;
import io.psyllabus.platform.common.exception.DuplicateResourceException;
import io.psyllabus.platform.common.exception.ResourceNotFoundException;
import io.psyllabus.platform.common.util.TenantContext;
import io.psyllabus.platform.school.dto.request.SchoolCreateRequest;
import io.psyllabus.platform.school.dto.responses.SchoolOnboardResponse;
import io.psyllabus.platform.school.dto.responses.SchoolResponse;
import io.psyllabus.platform.school.entities.School;
import io.psyllabus.platform.school.enums.SubscriptionPlan;
import io.psyllabus.platform.school.repository.SchoolRepository;
import io.psyllabus.platform.subscription.entity.Subscription;
import io.psyllabus.platform.subscription.enums.PlanType;
import io.psyllabus.platform.subscription.enums.SubscriptionStatus;
import io.psyllabus.platform.subscription.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * SchoolServiceImpl — this class IS the Facade pattern for school provisioning.
 *
 * Think of it as an assembly line. Each step is a station:
 *
 *   [1] School created      → the tenant record. Everything else hangs off its ID.
 *   [2] Subscription        → defines which features the school gets.
 *   [3] Main Branch         → every school starts with one campus.
 *   [4] Admin User          → created inside the new tenant context.
 *
 * The controller sees ONE method call: onboardSchool(request).
 * It knows nothing about Subscription, Branch, AuthService, or TenantContext.
 * That's what makes this a Facade.
 *
 * Transactional wraps all 4 stations: if admin creation fails (e.g. duplicate email),
 * the school, subscription, and branch all roll back — no orphaned tenant records.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SchoolServiceImpl implements SchoolService{

    private final SchoolRepository schoolRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final BranchRepository branchRepository;
    private final AuthService authService;

    @Value("${tenant.domain.base:psyllabus.co.ke}")
    private String baseDomain;

    /**
     * Temp password given to all provisioned admin accounts on first setup.
     * Admin is expected to change this on first login (password change sprint).
     */
    private static final String ADMIN_TEMP_PASSWORD = "Admin@123";

    @Override
    public SchoolOnboardResponse onboardSchool(SchoolCreateRequest request) {
        log.info("Provisioning school: subdomain={}, plan={}", request.subdomainPrefix(), request.planType());

        // Guard: uniqueness checks before writing anything
        if (schoolRepository.existsBySubdomainPrefix(request.subdomainPrefix())){
            throw new DuplicateResourceException("Subdomain already taken: " + request.subdomainPrefix());
        }
        if (schoolRepository.existsByEmail(request.email())){
            throw new DuplicateResourceException("Email already registered: " + request.email());
        }

        // Station : School (the tenant)
        School school = School.builder()
                .name(request.name())
                .subdomainPrefix(request.subdomainPrefix())
                .email(request.email())
                .phone(request.phone())
                .address(request.address())
                .plan(mapPlanType(request.planType()))
                .isActive(true)
                .build();
        school = schoolRepository.save(school);
        UUID schoolId = school.getId();
        log.info("School created: id={}", schoolId);

        // Station 2: Subscription (plan -> feature list)
        Subscription subscription = Subscription.builder()
                .tenantId(schoolId)
                .planType(request.planType())
                .startDate(LocalDateTime.now())
                .endDate(null) // TODO: expiry will be added when billin is wired
                .features(featuresFor(request.planType()))
                .status(SubscriptionStatus.ACTIVE)
                .isActive(true)
                .build();
        subscriptionRepository.save(subscription);
        log.info("Subscription created: plan={} for school={}", request.planType(), school);

        // Station 3: Main Branch creation
        Branch mainBranch = Branch.builder()
                .tenantId(schoolId)
                .name("Main Campus")
                .code("MAIN")
                .isMainBranch(true)
                .isActive(true)
                .build();

        mainBranch = branchRepository.save(mainBranch);
        log.info("Main branch created: id={} for school={}", mainBranch.getId(), schoolId);

        // Station 4: Admin User - AuthService.register() reads TenantContext
        // sp we set it manually for this unauthenticated provisioning call
        TenantContext.setCurrentTenant(schoolId);
        TenantContext.setCurrentTenant(schoolId);
        try{
            RegisterRequest adminRequest = new RegisterRequest(
                    request.adminEmail(), // username = email for admins
                    request.adminEmail(),
                    ADMIN_TEMP_PASSWORD,
                    request.adminFirstName(),
                    request.adminLastName(),
                    request.adminPhone(),
                    UserType.ADMIN,
                    mainBranch.getId(),
                    AccessScope.SCHOOL
            );
            authService.register(adminRequest);
            log.info("Admin user created for school={}", schoolId);
        }finally {
            // Always clear -- this request has no persistent tenant context
            TenantContext.clear();
        }
        log.info("School provisioning complete: {}", school.getName());

        return new SchoolOnboardResponse(
                school.getId(),
                school.getName(),
                school.getSubdomainPrefix(),
                request.adminEmail(),
                ADMIN_TEMP_PASSWORD,
                "https://" + request.subdomainPrefix() + "." + baseDomain
        );
    }

    @Override
    @Transactional(readOnly = true)
    public SchoolResponse findById(UUID schoolId) {
        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("School not found: " + schoolId));
        return mapToResponse(school);
    }

    @Override
    public SchoolResponse findBySubdomain(String subdomainPrefix) {
        School school = schoolRepository.findBySubdomainPrefix(subdomainPrefix)
                .orElseThrow(() -> new ResourceNotFoundException("School not found: " + subdomainPrefix));
        return mapToResponse(school);
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    /**
     * Factory Method (inline) — maps entity to response DTO.
     * Kept private: callers never know what fields the School entity has.
     */
    private SchoolResponse mapToResponse(School school) {
        return new SchoolResponse(
                school.getId(),
                school.getName(),
                school.getSubdomainPrefix(),
                school.getEmail(),
                school.getPlan(),
                school.getIsActive()
        );
    }

    private SubscriptionPlan mapPlanType(PlanType planType) {
        return switch (planType) {
            case BASIC        -> SubscriptionPlan.BASIC;
            case PROFESSIONAL -> SubscriptionPlan.PROFESSIONAL;
            case ENTERPRISE   -> SubscriptionPlan.ENTERPRISE;
        };
    }

    /**
     * Determines which features a school gets based on their plan.
     *
     * Tier logic (from product docs):
     *   BASIC        → Core academic management only
     *   PROFESSIONAL → Core + AI modules (Teacher AI, Parent AI, institutional analytics)
     *   ENTERPRISE   → Pro + advanced AI, multi-campus, custom reports, dedicated account manager
     *
     * Each higher tier is a superset — ENTERPRISE has everything PROFESSIONAL has.
     * This avoids silent gaps where an enterprise school is missing a pro feature.
     */
    private List<String> featuresFor(PlanType planType){
        List<String> core = List.of(
                FeatureConstants.STUDENT_MANAGEMENT,
                FeatureConstants.STAFF_MANAGEMENT,
                FeatureConstants.ATTENDANCE,
                FeatureConstants.GRADES,
                FeatureConstants.ANNOUNCEMENTS,
                FeatureConstants.ASSIGNMENTS,
                FeatureConstants.FEES_TRACKING,
                FeatureConstants.EMAIL_NOTIFICATIONS
        );

        return switch (planType){
            case BASIC -> core;

            case PROFESSIONAL -> {
                List<String> features = new ArrayList<>(core);
                features.addAll(List.of(
                        FeatureConstants.ADVANCED_ANALYTICS,
                        FeatureConstants.TEACHER_INTERVENTION,   // Teacher AI
                        FeatureConstants.STUDENT_GUIDANCE,       // Parent AI
                        FeatureConstants.INSTITUTIONAL_AI,       // wait — see note below
                        FeatureConstants.SMS_NOTIFICATIONS,
                        FeatureConstants.SCHOOL_BRANDING,
                        FeatureConstants.PRIORITY_SUPPORT
                ));
                yield features;
            }
            case ENTERPRISE -> {
                List<String> features = new ArrayList<>(core);
                features.addAll(List.of(
                        // Everything from PRO
                        FeatureConstants.ADVANCED_ANALYTICS,
                        FeatureConstants.TEACHER_INTERVENTION,
                        FeatureConstants.STUDENT_GUIDANCE,
                        FeatureConstants.INSTITUTIONAL_AI,
                        FeatureConstants.SMS_NOTIFICATIONS,
                        FeatureConstants.SCHOOL_BRANDING,
                        FeatureConstants.PRIORITY_SUPPORT,
                        // Enterprise-only additions
                        FeatureConstants.PREDICTIVE_ANALYTICS,
                        FeatureConstants.CUSTOM_REPORTS,
                        FeatureConstants.CUSTOM_DASHBOARDS,
                        FeatureConstants.BULK_SMS,
                        FeatureConstants.MULTI_CAMPUS,
                        FeatureConstants.CUSTOM_DOMAINS,
                        FeatureConstants.ADVANCED_EXPORTS,
                        FeatureConstants.DEDICATED_SUPPORT
                ));
                yield features;
            }
        };
    }
}
