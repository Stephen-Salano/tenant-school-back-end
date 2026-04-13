package io.psyllabus.platform.auth.services;

import io.psyllabus.platform.auth.dto.responses.ClaimsResponse;
import io.psyllabus.platform.auth.dto.responses.LinkedStudentClaim;
import io.psyllabus.platform.auth.entities.User;
import io.psyllabus.platform.auth.repository.UserRepository;
import io.psyllabus.platform.academic.dto.response.TeacherAssignmentResponse;
import io.psyllabus.platform.academic.repository.TeacherAssignmentRepository;
import io.psyllabus.platform.branch.entities.Branch;
import io.psyllabus.platform.branch.repository.BranchRepository;
import io.psyllabus.platform.common.exception.ResourceNotFoundException;
import io.psyllabus.platform.common.exception.SubscriptionException;
import io.psyllabus.platform.common.exception.TenantNotFoundException;
import io.psyllabus.platform.guardian.entities.Guardian;
import io.psyllabus.platform.guardian.entities.StudentGuardian;
import io.psyllabus.platform.guardian.repository.GuardianRepository;
import io.psyllabus.platform.guardian.repository.StudentGuardianRepository;
import io.psyllabus.platform.school.entities.School;
import io.psyllabus.platform.school.repository.SchoolRepository;
import io.psyllabus.platform.staff.entities.Staff;
import io.psyllabus.platform.staff.repository.StaffRepository;
import io.psyllabus.platform.student.entities.Student;
import io.psyllabus.platform.student.repository.StudentRepository;
import io.psyllabus.platform.subscription.entity.Subscription;
import io.psyllabus.platform.subscription.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * ClaimsService — post-login session bootstrap.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ClaimsServiceImpl implements ClaimsService {
  
  private final UserRepository userRepository;
  private final SchoolRepository schoolRepository;
  private final BranchRepository branchRepository;
  private final SubscriptionRepository subscriptionRepository;
  private final StaffRepository staffRepository;
  private final StudentRepository studentRepository;
  private final GuardianRepository guardianRepository;
  private final StudentGuardianRepository studentGuardianRepository;
  private final TeacherAssignmentRepository teacherAssignmentRepository;

  @Value("${spring.application.frontend-url}")
  private String frontendBaseUrl;
  
  @Override
  @Cacheable(value = "userClaims", key = "#userId", unless = "#result == null")
  public ClaimsResponse getFullClaims(UUID userId) {
    log.debug("Fetching full claims for user: {}", userId);
    
    User user = userRepository.findById(userId)
      .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    
    School school = schoolRepository.findById(user.getTenantId())
      .orElseThrow(() -> new TenantNotFoundException("School not found for tenant: " + user.getTenantId()));
    
    Branch branch = resolveBranch(user);
    Subscription subscription = resolveSubscription(user);

    return buildClaimsResponse(user, school, branch, subscription);
  }

  private Branch resolveBranch(User user) {
      if (user.getBranchId() != null) {
          return branchRepository.findById(user.getBranchId()).orElse(null);
      }
      return null;
  }

  private Subscription resolveSubscription(User user) {
      return subscriptionRepository
              .findByTenantIdAndIsActiveTrue(user.getTenantId())
              .orElseThrow(() -> new SubscriptionException("No active subscription for tenant: " + user.getTenantId()));
  }

  private ClaimsResponse buildClaimsResponse(User user, School school, Branch branch, Subscription subscription) {
      Staff staff = null;
      Student student = null;
      Guardian guardian = null;
      List<TeacherAssignmentResponse> teacherAssignments = List.of();
      List<LinkedStudentClaim> linkedStudents = List.of();

      // Population based on user type — moved to helper to reduce cognitive complexity
      switch (user.getUserType()) {
          case STAFF -> {
              staff = staffRepository.findByUserIdAndTenantId(user.getId(), user.getTenantId()).orElse(null);
              if (staff != null) {
                  teacherAssignments = teacherAssignmentRepository.findResponsesByStaffId(staff.getId(), user.getTenantId());
              }
          }
          case STUDENT -> student = studentRepository.findByUserIdAndTenantId(user.getId(), user.getTenantId()).orElse(null);
          case PARENT -> {
              guardian = guardianRepository.findByUserIdAndTenantId(user.getId(), user.getTenantId()).orElse(null);
              if (guardian != null) {
                  linkedStudents = fetchLinkedStudents(guardian, user.getTenantId());
              }
          }
          default -> log.debug("User type {} uses generic claim payload.", user.getUserType());
      }

      return new ClaimsResponse(
              user.getId(),
              user.getFirstName(),
              user.getLastName(),
              user.getUserType(),
              school.getId(),
              school.getName(),
              user.getBranchId(),
              branch != null ? branch.getName() : null,
              user.getAccessScope(),
              subscription.getPlanType(),
              subscription.getFeatures(),
              subscription.isValid(),
              determineDashboardUrl(user),
              determineDefaultPage(user),
              staff != null ? staff.getId() : null,
              staff != null ? staff.getSubjectGroupId() : null,
              teacherAssignments,
              student != null ? student.getId() : null,
              student != null ? student.getAdmissionNumber() : null,
              student != null ? student.getCurrentClassId() : null,
              guardian != null ? guardian.getId() : null,
              linkedStudents
      );
  }

  private List<LinkedStudentClaim> fetchLinkedStudents(Guardian guardian, UUID tenantId) {
      List<UUID> studentIds = studentGuardianRepository.findByGuardianIdAndTenantId(guardian.getId(), tenantId)
              .stream()
              .map(StudentGuardian::getStudentId)
              .toList();
      
      if (studentIds.isEmpty()) return List.of();

      return studentRepository.findAllByIdInAndTenantId(studentIds, tenantId)
              .stream()
              .map(this::toLinkedStudentClaim)
              .toList();
  }
  
  private String determineDashboardUrl(User user) {
    String path = switch (user.getUserType()){
      case ADMIN -> "/admin/dashboard";
      case STAFF -> "/teacher/dashboard";
      case PARENT -> "/parent/dashboard";
      case STUDENT -> "/student/dashboard";
      default -> "/dashboard";
    };
    return frontendBaseUrl + path;
  }
  
  private String determineDefaultPage(User user) {
    return switch (user.getUserType()) {
      case ADMIN -> "analytics";
      case STAFF -> "classes";
      case PARENT -> "child-progress";
      case STUDENT -> "assignments";
      default -> "home";
    };
  }

  private LinkedStudentClaim toLinkedStudentClaim(Student student) {
    return new LinkedStudentClaim(
            student.getId(),
            student.getAdmissionNumber(),
            student.getFirstName(),
            student.getLastName(),
            student.getCurrentClassId()
    );
  }
}
