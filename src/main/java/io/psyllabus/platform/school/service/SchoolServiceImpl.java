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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SchoolServiceImpl implements SchoolService{

    private final SchoolRepository schoolRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final BranchRepository branchRepository;
    private final AuthService authService;

    @Override
    public SchoolResponse onboardSchool(SchoolCreateRequest request) {
        log.info("Onboarding school: {}", request.email());

        // Validate uniqueness
        if (schoolRepository.existsBySubdomainPrefix(request.subdomainPrefix())){
            throw new DuplicateResourceException(" Subdomain prefix already exists");
        }

        if (schoolRepository.existsByEmail(request.email())){
            throw new DuplicateResourceException("Email already exists");
        }

        // step 1: Create school
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

        log.info("School created: {} ({})", school.getName(), schoolId);

        // Ste 2 Create Subscription
        Subscription subscription = createSubscription(schoolId, request.planType());
        subscriptionRepository.save(subscription);

        log.info(" Subscription created for school: {}", schoolId);

        // Step 3: Create main Branch
        Branch mainBranch = Branch.builder()
                .tenantId(schoolId)
                .name("Main Campus")
                .code("Main")
                .isMainBranch(true)
                .build();

        mainBranch = branchRepository.save(mainBranch);
        log.info("Main branch created for school: {}", schoolId);

        // Step 4: Create Admin User
        // Set TenantContext temporarily for admin creation
        TenantContext.setCurrentTenant(schoolId);

        try{
            RegisterRequest adminRequest = new RegisterRequest(
                    request.adminEmail(), // username = email
                    request.adminEmail(), // actual email field
                    "Admin@123", // default admin password
                    request.adminFirstName(),
                    request.adminLastName(),
                    request.adminPhone(),
                    UserType.ADMIN,
                    mainBranch.getId(),
                    AccessScope.SCHOOL
            );

            authService.register(adminRequest);

            log.info("Adminn user created for school: {}", schoolId);
        } finally {
            TenantContext.clear();
        }

        log.info("School onboarding complete: {}", school.getName());

        return mapToResponse(school);
    }

    @Override
    public SchoolResponse findBySubdomain(String subdomainPrefix) {
        School school = schoolRepository.findBySubdomainPrefix(subdomainPrefix)
                .orElseThrow(() -> new ResourceNotFoundException("School not found"));

        return mapToResponse(school);
    }

    @Override
    public SchoolResponse findById(UUID schoolId) {
        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() ->  new ResourceNotFoundException(" School not found"));

        return mapToResponse(school);
    }

    // TODO: Migrate to Subcsription Service
    private Subscription createSubscription(UUID tenantId, PlanType planType){
        List<String> features = getFeaturesForPlan(planType);

        return Subscription.builder()
                .tenantId(tenantId)
                .planType(planType)
                .startDate(LocalDateTime.now())
                .endDate(null)
                .features(features)
                .status(SubscriptionStatus.ACTIVE)
                .isActive(true)
                .build();
    }

    private List<String> getFeaturesForPlan(PlanType planType) {
        return switch (planType) {
            case BASIC -> List.of(
                // core operational features
                    "student_management",
                    "staff_management",
                    "attendance",
                    "grades",
                    "announcements",
                    "assignments",
                    "fees_tracking",
                    "extracurricular_tracking",
                    FeatureConstants.EMAIL_NOTIFICATIONS
                    // NO AI, NO advanced analytics
            );

            case PROFESSIONAL -> List.of(
                    // All BASIC features
                    "student_management",
                    "staff_management",
                    "attendance",
                    "grades",
                    "announcements",
                    "assignments",
                    "fees_tracking",
                    "extracurricular_tracking",
                    FeatureConstants.EMAIL_NOTIFICATIONS,
                    // PROFESSIONAL-exclusive features
                    FeatureConstants.ADVANCED_ANALYTICS,
                    FeatureConstants.TEACHER_INTERVENTION,
                    FeatureConstants.STUDENT_GUIDANCE,
                    FeatureConstants.SMS_NOTIFICATIONS,
                    FeatureConstants.SCHOOL_BRANDING,
                    FeatureConstants.PRIORITY_SUPPORT,
                    FeatureConstants.SCHOOL_BRANDING
            );

            case ENTERPRISE -> List.of(
                    // All BASIC features
                    "student_management",
                    "staff_management",
                    "attendance",
                    "grades",
                    "announcements",
                    "assignments",
                    "fees_tracking",
                    "extracurricular_tracking",
                    FeatureConstants.EMAIL_NOTIFICATIONS,
                    // All PROFESSIONAL features
                    FeatureConstants.ADVANCED_ANALYTICS,
                    FeatureConstants.TEACHER_INTERVENTION,
                    FeatureConstants.STUDENT_GUIDANCE,
                    FeatureConstants.SMS_NOTIFICATIONS,
                    FeatureConstants.SCHOOL_BRANDING,
                    FeatureConstants.PRIORITY_SUPPORT,
                    // ENTERPRISE-exclusive features
                    FeatureConstants.INSTITUTIONAL_AI,
                    FeatureConstants.PREDICTIVE_ANALYTICS,
                    FeatureConstants.CUSTOM_REPORTS,
                    FeatureConstants.CUSTOM_DASHBOARDS,
                    FeatureConstants.MULTI_CAMPUS,
                    FeatureConstants.CUSTOM_DOMAINS,
                    FeatureConstants.BULK_SMS,
                    FeatureConstants.ADVANCED_EXPORTS,
                    FeatureConstants.DEDICATED_SUPPORT
            );
        };
    }

    private SubscriptionPlan mapPlanType(PlanType planType) {
        return switch (planType) {
            case BASIC -> SubscriptionPlan.BASIC;
            case PROFESSIONAL -> SubscriptionPlan.PROFESSIONAL;
            case ENTERPRISE -> SubscriptionPlan.ENTERPRISE;
        };
    }


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
}
