package io.psyllabus.platform.school.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.psyllabus.platform.auth.dto.request.RegisterRequest;
import io.psyllabus.platform.auth.enums.AccessScope;
import io.psyllabus.platform.auth.enums.UserType;
import io.psyllabus.platform.auth.services.AuthService;
import io.psyllabus.platform.branch.entities.Branch;
import io.psyllabus.platform.branch.repository.BranchRepository;
import io.psyllabus.platform.common.constants.FeatureConstants;
import io.psyllabus.platform.common.exception.DuplicateResourceException;
import io.psyllabus.platform.common.exception.InsufficientPermissionException;
import io.psyllabus.platform.common.exception.ResourceNotFoundException;
import io.psyllabus.platform.common.exception.ValidationException;
import io.psyllabus.platform.common.util.RandomUtils;
import io.psyllabus.platform.common.util.TenantContext;
import io.psyllabus.platform.school.dto.request.SchoolCreateRequest;
import io.psyllabus.platform.school.dto.request.SchoolProfileUpdateRequest;
import io.psyllabus.platform.school.dto.responses.SchoolOnboardResponse;
import io.psyllabus.platform.school.dto.responses.SchoolProfileResponse;
import io.psyllabus.platform.school.entities.School;
import io.psyllabus.platform.school.entities.SchoolProfile;
import io.psyllabus.platform.school.repository.SchoolProfileRepository;
import io.psyllabus.platform.school.repository.SchoolRepository;
import io.psyllabus.platform.subscription.entity.Subscription;
import io.psyllabus.platform.subscription.enums.PlanType;
import io.psyllabus.platform.subscription.enums.SubscriptionStatus;
import io.psyllabus.platform.subscription.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * SchoolServiceImpl — Facade pattern for school provisioning and profile management.
 *
 * <p>Think of it as an assembly line for provisioning. Each step is a station:</p>
 * <ol>
 *   <li>School created → the tenant record; everything else hangs off its ID.</li>
 *   <li>Subscription   → defines which features the school gets.</li>
 *   <li>Main Branch    → every school starts with one campus.</li>
 *   <li>Admin User     → created inside the new tenant context.</li>
 * </ol>
 *
 * <p>The controller sees ONE method call: {@code onboardSchool(request)}.
 * It knows nothing about Subscription, Branch, AuthService, or TenantContext.</p>
 *
 * <p>{@code @Transactional} wraps all 4 stations: if admin creation fails
 * (e.g. duplicate email), the school, subscription, and branch all roll back —
 * no orphaned tenant records.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SchoolServiceImpl implements SchoolService{

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final SchoolRepository schoolRepository;
    private final SchoolProfileRepository schoolProfileRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final BranchRepository branchRepository;
    private final AuthService authService;
    private final ObjectMapper objectMapper;

    @Value("${tenant.domain.base:psyllabus.co.ke}")
    private String baseDomain;

    // ── Provisioning ──────────────────────────────────────────────────────────

    @Override
    public SchoolOnboardResponse onboardSchool(SchoolCreateRequest request) {
        String password = RandomUtils.generateRandomAlphanumeric(12);
        log.info("Provisioning school: subdomain={}, plan={}", request.subdomainPrefix(), request.planType());

        boolean hasSubdomain = request.subdomainPrefix() != null && !request.subdomainPrefix().isBlank();
        boolean hasCustomDomain = request.customDomain() != null && !request.customDomain().isBlank();

        if (!hasSubdomain && !hasCustomDomain) {
            throw new ValidationException("Either subdomainPrefix or customDomain must be provided");
        }
        if (hasSubdomain && hasCustomDomain) {
            throw new ValidationException("Provide either subdomainPrefix or customDomain — not both");
        }
        if (hasSubdomain && schoolRepository.existsBySubdomainPrefix(request.subdomainPrefix())) {
            throw new DuplicateResourceException("Subdomain already taken: " + request.subdomainPrefix());
        }
        if (schoolRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Email already registered: " + request.email());
        }

        // Station 1: School (the tenant)
        School school = School.builder()
                .name(request.name())
                .subdomainPrefix(request.subdomainPrefix())
                .email(request.email())
                .phone(request.phone())
                .address(request.address())
                .customDomain(request.customDomain())
                .plan(request.planType())
                .isActive(true)
                .build();
        school = schoolRepository.save(school);
        UUID schoolId = school.getId();
        log.info("School created: id={}", schoolId);

        // Station 2: Subscription (plan → feature list)
        Subscription subscription = Subscription.builder()
                .tenantId(schoolId)
                .planType(request.planType())
                .startDate(LocalDateTime.now())
                .endDate(null) // populated when billing is wired
                .features(featuresFor(request.planType()))
                .status(SubscriptionStatus.ACTIVE)
                .isActive(true)
                .build();
        subscriptionRepository.save(subscription);
        log.info("Subscription created: plan={} for school={}", request.planType(), schoolId);

        // Station 3: Main Branch
        Branch mainBranch = Branch.builder()
                .tenantId(schoolId)
                .name("Main Campus")
                .code("MAIN")
                .isMainBranch(true)
                .isActive(true)
                .build();
        mainBranch = branchRepository.save(mainBranch);
        log.info("Main branch created: id={} for school={}", mainBranch.getId(), schoolId);

        // Station 4: Admin User — AuthService.register() reads TenantContext,
        // so we set it manually for this unauthenticated provisioning call.
        TenantContext.setCurrentTenant(schoolId);
        try {
            RegisterRequest adminRequest = new RegisterRequest(
                    request.adminEmail(),
                    request.adminEmail(),
                    password,
                    request.adminFirstName(),
                    request.adminLastName(),
                    request.adminPhone(),
                    UserType.ADMIN,
                    mainBranch.getId(),
                    AccessScope.SCHOOL
            );
            authService.register(adminRequest);
            log.info("Admin user created for school={}", schoolId);
        } finally {
            TenantContext.clear();
        }

        log.info("School provisioning complete: {}", school.getName());

        return new SchoolOnboardResponse(
                school.getId(),
                school.getName(),
                school.getSubdomainPrefix(),
                request.adminEmail(),
                password,
                buildPortalUrl(request),
                request.planType(),
                LocalDateTime.now(ZoneOffset.UTC),
                null
        );
    }

    // ── Tenant resolution (internal infrastructure) ───────────────────────────

    @Override
    @Transactional(readOnly = true)
    public School findBySubdomain(String subdomainPrefix) {
        return schoolRepository.findBySubdomainPrefix(subdomainPrefix)
                .orElseThrow(() -> new ResourceNotFoundException("School not found for subdomain: " + subdomainPrefix));
    }

    // ── School profile ────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "schoolProfiles", key = "#schoolId")
    public SchoolProfileResponse getSchoolProfile(UUID schoolId) {
        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("School not found: " + schoolId));

        SchoolProfile profile = schoolProfileRepository.findBySchoolId(schoolId)
                .orElse(null); // profile is null until the admin completes the settings wizard

        log.debug("Fetching school profile: schoolId={}, profileExists={}", schoolId, profile != null);
        return mapToProfileResponse(school, profile);
    }

    @Override
    @CacheEvict(value = "schoolProfiles", key = "#schoolId")
    public SchoolProfileResponse updateSchoolProfile(UUID schoolId, SchoolProfileUpdateRequest request) {
        UUID currentTenantId = TenantContext.getCurrentTenantId();
        if (!schoolId.equals(currentTenantId)) {
            throw new InsufficientPermissionException("Cannot update another school's profile");
        }

        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("School not found: " + schoolId));

        // Fetch or create the profile row — first time an admin saves settings,
        // the profile row will not exist yet.
        SchoolProfile profile = schoolProfileRepository.findBySchoolId(schoolId)
                .orElseGet(() -> {
                    log.info("Creating initial SchoolProfile row for school={}", schoolId);
                    SchoolProfile newProfile = new SchoolProfile();
                    newProfile.setSchoolId(schoolId);
                    newProfile.setSchool(school);
                    return newProfile;
                });

        // Apply non-null sections only (PATCH semantics).
        applyGeneralSection(profile, request.general());
        applyAcademicSection(profile, request.academic());
        applyEnrollmentSection(profile, request.enrollment());
        applyJsonbSection(profile, request.facilities());
        applyJsonbSection(profile, request.technology());
        applyJsonbSection(profile, request.policies());
        applyJsonbSection(profile, request.communication());

        schoolProfileRepository.save(profile);
        log.info("SchoolProfile updated: schoolId={}", schoolId);

        return mapToProfileResponse(school, profile);
    }

    // ── Section appliers (PATCH semantics) ───────────────────────────────────

    /**
     * Applies non-null fields from the General section to the entity.
     * Column-mapped fields are merged field-by-field.
     */
    private void applyGeneralSection(SchoolProfile profile, SchoolProfileUpdateRequest.GeneralSection section) {
        if (section == null) return;
        if (section.schoolCode() != null)     profile.setSchoolCode(section.schoolCode());
        if (section.schoolType() != null)     profile.setSchoolType(section.schoolType());
        if (section.genderPolicy() != null)   profile.setGenderPolicy(section.genderPolicy());
        if (section.logoUrl() != null)        profile.setLogoUrl(section.logoUrl());
        if (section.website() != null)        profile.setWebsite(section.website());
        if (section.establishedYear() != null) profile.setEstablishedYear(section.establishedYear());
        if (section.motto() != null)          profile.setMotto(section.motto());
        if (section.vision() != null)         profile.setVision(section.vision());
        if (section.mission() != null)        profile.setMission(section.mission());
        if (section.brandColors() != null)    profile.setBrandColors(section.brandColors());
    }

    /**
     * Applies non-null fields from the Academic section to the entity.
     */
    private void applyAcademicSection(SchoolProfile profile, SchoolProfileUpdateRequest.AcademicSection section) {
        if (section == null) return;
        if (section.curriculumType() != null)        profile.setCurriculumType(section.curriculumType());
        if (section.schoolDays() != null)            profile.setSchoolDays(section.schoolDays());
        if (section.startTime() != null)             profile.setStartTime(section.startTime());
        if (section.endTime() != null)               profile.setEndTime(section.endTime());
        if (section.lunchStartTime() != null)        profile.setLunchStartTime(section.lunchStartTime());
        if (section.lunchEndTime() != null)          profile.setLunchEndTime(section.lunchEndTime());
        if (section.useSystemLearningAreas() != null) profile.setUseSystemLearningAreas(section.useSystemLearningAreas());
    }

    /**
     * Applies non-null fields from the Enrollment section to the entity.
     * currentEnrollment and availableSlots are read-only computed values —
     * they are never accepted in a write request.
     */
    private void applyEnrollmentSection(SchoolProfile profile, SchoolProfileUpdateRequest.EnrollmentSection section) {
        if (section == null) return;
        if (section.totalCapacity() != null)      profile.setTotalCapacity(section.totalCapacity());
        if (section.maxClassSize() != null)       profile.setMaxClassSize(section.maxClassSize());
        if (section.studentTeacherRatio() != null) profile.setStudentTeacherRatio(section.studentTeacherRatio());
        if (section.averageClassSize() != null)   profile.setAverageClassSize(section.averageClassSize());
        if (section.gradesOffered() != null)      profile.setGradesOffered(section.gradesOffered());
    }

    /**
     * Applies a JSONB tab section to its corresponding entity field.
     * JSONB sections are replaced at the tab level — the entire blob is overwritten
     * when the section is present. This matches the UI's tab-level save behaviour.
     *
     * <p>Uses a sealed dispatch on the section type — adding a new JSONB tab
     * requires a new branch here and in the entity.</p>
     */
    private void applyJsonbSection(SchoolProfile profile, Object section) {
        if (section == null) return;

        switch (section) {
            case SchoolProfileUpdateRequest.FacilitiesSection s ->
                    profile.setFacilities(objectMapper.convertValue(s, MAP_TYPE));
            case SchoolProfileUpdateRequest.TechnologySection s ->
                    profile.setTechnology(objectMapper.convertValue(s, MAP_TYPE));
            case SchoolProfileUpdateRequest.PoliciesSection s ->
                    profile.setPolicies(objectMapper.convertValue(s, MAP_TYPE));
            case SchoolProfileUpdateRequest.CommunicationSection s ->
                    profile.setCommunication(objectMapper.convertValue(s, MAP_TYPE));
            default -> log.warn("applyJsonbSection: unrecognised section type {}", section.getClass().getSimpleName());
        }
    }

    // ── Response mapper ───────────────────────────────────────────────────────

    /**
     * Maps {@link School} + {@link SchoolProfile} into a single {@link SchoolProfileResponse}.
     *
     * <p>The profile may be {@code null} when the admin has not yet configured settings —
     * all tab sections will be {@code null} in that case and the frontend renders defaults.</p>
     *
     * <p>JSONB blobs are converted back into typed nested records using
     * {@link ObjectMapper#convertValue}. Column-mapped fields are set directly.</p>
     *
     * <p>TODO (SM4): Replace the stubbed {@code currentEnrollment = 0} with a real count
     * from {@code StudentRepository.countActiveByTenantId(schoolId)} once that
     * repository exists.</p>
     */
    private SchoolProfileResponse mapToProfileResponse(School school, SchoolProfile profile) {
        SchoolProfileResponse.GeneralSection general = null;
        SchoolProfileResponse.AcademicSection academic = null;
        SchoolProfileResponse.EnrollmentSection enrollment = null;
        SchoolProfileResponse.FacilitiesSection facilities = null;
        SchoolProfileResponse.TechnologySection technology = null;
        SchoolProfileResponse.PoliciesSection policies = null;
        SchoolProfileResponse.CommunicationSection communication = null;

        if (profile != null) {
            general = new SchoolProfileResponse.GeneralSection(
                    profile.getSchoolCode(),
                    profile.getSchoolType(),
                    profile.getGenderPolicy(),
                    profile.getLogoUrl(),
                    profile.getWebsite(),
                    profile.getEstablishedYear(),
                    profile.getMotto(),
                    profile.getVision(),
                    profile.getMission(),
                    profile.getBrandColors()
            );

            academic = new SchoolProfileResponse.AcademicSection(
                    profile.getCurriculumType(),
                    profile.getSchoolDays(),
                    profile.getStartTime(),
                    profile.getEndTime(),
                    profile.getLunchStartTime(),
                    profile.getLunchEndTime(),
                    profile.isUseSystemLearningAreas()
            );

            // TODO (SM4): replace stub with StudentRepository.countActiveByTenantId(school.getId())
            int currentEnrollment = 0;
            int availableSlots = profile.getTotalCapacity() != null
                    ? Math.max(0, profile.getTotalCapacity() - currentEnrollment)
                    : 0;

            enrollment = new SchoolProfileResponse.EnrollmentSection(
                    profile.getTotalCapacity(),
                    currentEnrollment,
                    availableSlots,
                    profile.getMaxClassSize(),
                    profile.getStudentTeacherRatio(),
                    profile.getAverageClassSize(),
                    profile.getGradesOffered()
            );

            facilities = profile.getFacilities() != null
                    ? objectMapper.convertValue(profile.getFacilities(), SchoolProfileResponse.FacilitiesSection.class)
                    : null;

            technology = profile.getTechnology() != null
                    ? objectMapper.convertValue(profile.getTechnology(), SchoolProfileResponse.TechnologySection.class)
                    : null;

            policies = profile.getPolicies() != null
                    ? objectMapper.convertValue(profile.getPolicies(), SchoolProfileResponse.PoliciesSection.class)
                    : null;

            communication = profile.getCommunication() != null
                    ? objectMapper.convertValue(profile.getCommunication(), SchoolProfileResponse.CommunicationSection.class)
                    : null;
        }

        return new SchoolProfileResponse(
                school.getId(),
                school.getName(),
                school.getSubdomainPrefix(),
                school.getEmail(),
                school.getPhone(),
                school.getAddress(),
                school.getPlan(),
                school.getIsActive(),
                general,
                academic,
                enrollment,
                facilities,
                technology,
                policies,
                communication
        );
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Determines which features a school gets based on their plan.
     * Each higher tier is a strict superset — ENTERPRISE has everything PROFESSIONAL has.
     */
    private List<String> featuresFor(PlanType planType) {
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

        return switch (planType) {
            case BASIC -> core;
            case PROFESSIONAL -> {
                List<String> features = new ArrayList<>(core);
                features.addAll(List.of(
                        FeatureConstants.ADVANCED_ANALYTICS,
                        FeatureConstants.TEACHER_INTERVENTION,
                        FeatureConstants.STUDENT_GUIDANCE,
                        FeatureConstants.INSTITUTIONAL_AI,
                        FeatureConstants.SMS_NOTIFICATIONS,
                        FeatureConstants.SCHOOL_BRANDING,
                        FeatureConstants.PRIORITY_SUPPORT
                ));
                yield features;
            }
            case ENTERPRISE -> {
                List<String> features = new ArrayList<>(core);
                features.addAll(List.of(
                        FeatureConstants.ADVANCED_ANALYTICS,
                        FeatureConstants.TEACHER_INTERVENTION,
                        FeatureConstants.STUDENT_GUIDANCE,
                        FeatureConstants.INSTITUTIONAL_AI,
                        FeatureConstants.SMS_NOTIFICATIONS,
                        FeatureConstants.SCHOOL_BRANDING,
                        FeatureConstants.PRIORITY_SUPPORT,
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

    private String buildPortalUrl(SchoolCreateRequest request) {
        if (request.subdomainPrefix() != null && !request.subdomainPrefix().isBlank()) {
            return "https://" + request.subdomainPrefix() + "." + baseDomain;
        }
        return "https://" + request.customDomain();
    }
}
