package io.psyllabus.platform.auth.services;

import io.psyllabus.platform.auth.dto.request.ChangePasswordRequest;
import io.psyllabus.platform.auth.dto.request.UpdateProfileRequest;
import io.psyllabus.platform.auth.dto.responses.UserProfileResponse;
import io.psyllabus.platform.auth.entities.User;
import io.psyllabus.platform.auth.repository.UserRepository;
import io.psyllabus.platform.branch.entities.Branch;
import io.psyllabus.platform.branch.repository.BranchRepository;
import io.psyllabus.platform.common.exception.ResourceNotFoundException;
import io.psyllabus.platform.common.exception.ValidationException;
import io.psyllabus.platform.common.util.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * User profile management.
 *
 * Pattern notes:
 *
 *   Proxy pattern — @Cacheable("userProfile") on getProfile().
 *   Spring AOP creates a proxy that intercepts the method call.
 *   On a cache hit, the proxy returns cached data without touching the DB.
 *   On a cache miss, the real method runs and the result is stored.
 *   Visually: [Caller] → [Spring Proxy] → (hit) return cache
 *                                       → (miss) [UserServiceImpl] → DB → cache → return
 *
 *   CacheEvict on updateProfile() and changePassword() ensures the proxy
 *   doesn't serve stale data after a mutation.
 *
 * Deliberately thin — business logic stays in domain services (StaffService, StudentService, etc.)
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final BranchRepository branchRepository;
    private final PasswordService passwordService;

    @Override
    public User getById(UUID userId) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        return userRepository.findById(userId)
                .filter(u -> u.getTenantId().equals(tenantId))
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    }


    @Override
    @Cacheable(value = "userProfile", key = "#userId")
    public UserProfileResponse getProfile(UUID userId) {
        log.debug("Loading user profile from DB: {}", userId);
        User user = getById(userId);
        return mapToProfileResponse(user);
    }

    @Override
    @Transactional
    @CacheEvict(value = "userProfile", key = "#userId")
    public UserProfileResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = getById(userId);

        // Patch semantics - only apply fields that were actually sent
        if (request.firstName() != null) user.setFirstName(request.firstName());
        if (request.lastName() != null) user.setLastName(request.lastName());
        if (request.phone() != null) user.setPhone(request.phone());

        User saved = userRepository.save(user);
        log.info("Profile updated for user: {}", userId);
        return mapToProfileResponse(saved);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"userProfile", "userClaims"}, key = "#userId")
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        if (!request.newPassword().equals(request.confirmPassword())){
            throw new ValidationException("New password and confirmation do not match");
        }

        User user = getById(userId);

        if (!passwordService.verifyPassword(request.currentPassword(), user.getPassword())){
            throw new ValidationException("Current password is incorrect");
        }

        user.setHashedPassword(passwordService.hashPassword(request.newPassword()));
        userRepository.save(user);
        log.info("Password changed for user: {}", userId);
    }


    // ── Private helpers ────────────────────────────────────────────────────────
    /**
     * Factory Method (inline) — maps User entity to profile response.
     * Resolves branch name here to avoid N+1 from the frontend calling a separate endpoint.
     */
    private UserProfileResponse mapToProfileResponse(User user) {
        String branchName = null;
        if (user.getBranchId() != null) {
            branchName = branchRepository.findById(user.getBranchId())
                    .map(Branch::getName)
                    .orElse(null);
        }

        return new UserProfileResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getPhone(),
                user.getUserType(),
                user.getAccessScope(),
                user.getStatus(),
                user.getBranchId(),
                branchName
        );
    }
}
