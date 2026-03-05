package io.psyllabus.platform.auth.services;

import io.psyllabus.platform.auth.dto.responses.ClaimsResponse;
import io.psyllabus.platform.auth.entities.User;
import io.psyllabus.platform.auth.repository.UserRepository;
import io.psyllabus.platform.branch.entities.Branch;
import io.psyllabus.platform.branch.repository.BranchRepository;
import io.psyllabus.platform.common.exception.ResourceNotFoundException;
import io.psyllabus.platform.common.exception.SubscriptionException;
import io.psyllabus.platform.common.exception.TenantNotFoundException;
import io.psyllabus.platform.school.entities.School;
import io.psyllabus.platform.school.repository.SchoolRepository;
import io.psyllabus.platform.subscription.entity.Subscription;
import io.psyllabus.platform.subscription.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * ClaimsService — post-login session bootstrap.
 *
 * WHY THIS EXISTS:
 *   The JWT access token is intentionally minimal (userId + tenantId only).
 *   The frontend needs much more to render the dashboard: user name, school,
 *   branch, subscription plan, feature flags, and where to redirect.
 *
 * WHEN IT'S CALLED:
 *   Exactly once per login, immediately after POST /api/auth/login returns.
 *   Frontend calls GET /api/auth/claims, stores the response, and uses it
 *   for the entire session (no repeat calls unless the session is re-established).
 *
 * CACHING:
 *   Result is cached in Redis by userId (TTL: 5 minutes).
 *   Cache is evicted on: password change, profile update, subscription change.
 *   If you add any feature that modifies user/school/subscription state,
 *   add @CacheEvict(value = "userClaims", key = "#userId") to that method.
 *
 * DATA SOURCES:
 *   users → who the person is
 *   schools → which tenant (school) they belong to
 *   branches → which branch within that school (nullable)
 *   subscriptions → what plan and features are active
 *
 * MULTI-TENANCY:
 *   Tenant isolation is enforced by using user.getTenantId() to scope
 *   all lookups. A user from tenant A can never see tenant B's school or
 *   subscription data through this service.
 */


@Service
@RequiredArgsConstructor
@Slf4j
public class ClaimsServiceImpl implements ClaimsService {
  
  private final UserRepository userRepository;
  private final SchoolRepository schoolRepository;
  private final BranchRepository branchRepository;
  private final SubscriptionRepository subscriptionRepository;

  @Value("${spring.application.frontend-url}")
  private String frontendBaseUrl;
  
  @Override
  @Cacheable(value = "userClaims", key = "#userId", unless = "#result == null")
  public ClaimsResponse getFullClaims(UUID userId) {
    log.debug("Fetching full claims for user: {}", userId);
    
    // Load user
    User user = userRepository.findById(userId)
      .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    
    // Load school
    School school = schoolRepository.findById(user.getTenantId())
      .orElseThrow(() -> new TenantNotFoundException("School not found for tenant: " + user.getTenantId()));
    
    // Load branch (optional)
    Branch branch = null;
    if (user.getBranchId() != null) {
      branch = branchRepository.findById(user.getBranchId()).orElse(null);
    }
    
    // Load subscription
    Subscription subscription = subscriptionRepository
      .findByTenantIdAndIsActiveTrue(user.getTenantId())
      .orElseThrow(() -> new SubscriptionException("No active subscription for tenant: " + user.getTenantId()));

    log.debug("Claims built for user: {} (tenant: {})", userId, user.getTenantId());
    
    // Build response
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
      determineDefaultPage(user)
    );
  }
  
  private String determineDashboardUrl(User user) {
    String path = switch (user.getUserType()){
      case ADMIN -> "/admin/dashboard";
      case STAFF -> "/teacher/dashboard";
      case PARENT -> "/parent/dashboard";
      case STUDENT -> "/student/dashboard";
      default -> "/dashboard";
    };

    // e.g. "http://localhost:3000/teacher/dashboard" in dev, "https://app.psyllabus.io/admin/dashboard"  in prod
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
}
