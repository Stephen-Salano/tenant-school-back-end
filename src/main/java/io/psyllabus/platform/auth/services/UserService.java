package io.psyllabus.platform.auth.services;

import io.psyllabus.platform.auth.dto.request.ChangePasswordRequest;
import io.psyllabus.platform.auth.dto.request.UpdateProfileRequest;
import io.psyllabus.platform.auth.dto.responses.UserProfileResponse;
import io.psyllabus.platform.auth.entities.User;

import java.util.UUID;

/**
 * User profile management.
 * Auth (login/register) lives in AuthService.
 * Domain profiles (Staff, Student, Guardian) live in their own services.
 * This service handles cross-cutting user operations.
 */
public interface UserService {
    /**
     * Raw entity lookup — used internally by other services that need the User object.
     * Enforces tenant isolation: throws if the user belongs to a different tenant.
     */
    User getById(UUID userId);

    /**
     * Returns the current user's profile DTO.
     * Cacheable — profile data rarely changes mid-session.
     */
    UserProfileResponse getProfile(UUID userId);

    /**
     * Partial update — only non-null fields in the request are applied.
     * PATCH semantics: firstName/lastName/phone are optional.
     */
    UserProfileResponse updateProfile(UUID userId, UpdateProfileRequest request);

    /**
     * Change password.
     * Verifies currentPassword before applying the new hash.
     * Clears userClaims cache after update.
     */
    void changePassword(UUID userId, ChangePasswordRequest request);

}
