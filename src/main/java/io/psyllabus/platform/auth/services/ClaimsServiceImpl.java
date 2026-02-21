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
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClaimsServiceImpl implements ClaimsService {
  
  private final UserRepository userRepository;
  private final SchoolRepository schoolRepository;
  private final BranchRepository branchRepository;
  private final SubscriptionRepository subscriptionRepository;
  
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
    return switch (user.getUserType()) {
      case ADMIN -> "/dashboard/admin";
      case STAFF -> "/dashboard/teacher";
      case PARENT -> "/dashboard/parent";
      case STUDENT -> "/dashboard/student";
      default -> "/dashboard";
    };
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
