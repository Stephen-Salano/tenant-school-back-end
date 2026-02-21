package io.psyllabus.platform.auth.dto.responses;

import io.psyllabus.platform.auth.enums.AccessScope;
import io.psyllabus.platform.auth.enums.UserStatus;
import io.psyllabus.platform.auth.enums.UserType;

import java.util.UUID;

/**
 * Read-only view of the current user's profile.
 * Returned by GET /api/users/me.
 *
 * Intentionally excludes: password hash, internal tenant IDs beyond what's needed,
 * and security fields — minimum necessary data (K-DPA data minimization principle).
 */
public record UserProfileResponse (
        UUID userId,
        String firstName,
        String lastName,
        String email,
        String phone,
        UserType userType,
        AccessScope accessScope,
        UserStatus status,
        UUID branchId,
        String branchName   // resolved in service — avoids extra round-trip from frontend
){
}
