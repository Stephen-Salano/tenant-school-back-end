package io.psyllabus.platform.auth.services;

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
     * Get user by ID, scoped to current tenant.
     * Throws ResourceNotFoundException if not found or belongs to different tenant.
     */
    User getById(UUID userId);

}
