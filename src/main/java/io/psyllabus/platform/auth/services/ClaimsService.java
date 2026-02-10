package io.psyllabus.platform.auth.services;

import io.psyllabus.platform.auth.dto.responses.ClaimsResponse;
import java.util.UUID;

public interface ClaimsService {
  /**
   * Fetch full user claims (user details, tenant, branch, subscription).
   * This endpoint is called ONCE after login to populate dashboard.
   * 
   * @param userId from JWT
   * @return ClaimsResponse with all details
   */
  ClaimsResponse getFullClaims(UUID userId);
}
