package io.psyllabus.platform.auth.controller;

import io.psyllabus.platform.auth.dto.request.ChangePasswordRequest;
import io.psyllabus.platform.auth.dto.request.UpdateProfileRequest;
import io.psyllabus.platform.auth.dto.responses.UserProfileResponse;
import io.psyllabus.platform.auth.entities.User;
import io.psyllabus.platform.auth.services.UserService;
import io.psyllabus.platform.common.controller.BaseController;
import io.psyllabus.platform.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.Response;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

/**
 * UserController — current user's own profile operations.
 *
 * Extends BaseController (Template Method pattern):
 *   The "template" skeleton is: extract user → validate → call service → wrap response.
 *   This controller only fills in the domain-specific "call service" step.
 *
 * Scope: these endpoints are ONLY for the currently logged-in user acting on their own data.
 * Admin operations on other users (create, deactivate) live in StaffController / StudentController.
 *
 * All endpoints here are implicitly authenticated — SecurityConfig protects /api/users/** with JWT.
 */

@Controller
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "User Profile", description = "Current user profile management")
public class UserController extends BaseController {

    private final UserService userService;

    /**
     * GET /api/users/me
     *
     * Returns the current user's profile.
     * Used by the frontend on app load to populate the header/nav with name, role, branch.
     *
     * @Cacheable inside UserServiceImpl — fast after first load.
     */
    @GetMapping("/me")
    @Operation(summary = "Get current user profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getMyProfile(Authentication authentication){
        User currentUser = getCurrentUser(authentication);
        log.debug("Profile request for user: {}", currentUser.getId());
        UserProfileResponse profile = userService.getProfile(currentUser.getId());
        return ok(profile, "Profile retrieved");
    }

    /**
     * PATCH /api/users/me
     *
     * Partial update — only sends what changed (name, phone).
     * Fields not included in the request body are left unchanged.
     *
     * Evicts userProfile cache so next GET /me returns fresh data.
     */
    @PatchMapping("/me")
    @Operation(summary = "Update current user profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateMyProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateProfileRequest request
            ){
        User currentUser = getCurrentUser(authentication);
        log.info("Profile update for user: {}", currentUser.getId());
        UserProfileResponse updated = userService.updateProfile(currentUser.getId(), request);
        return ok(updated, "Profile updated successfully");
    }

    /**
     * POST /api/users/me/change-password
     *
     * Requires the current password as proof of identity.
     * Validates newPassword == confirmPassword before applying.
     * Evicts both userProfile and userClaims caches — claims embed the auth state.
     */
    @PostMapping("/me/change-password")
    @Operation(summary = "Change current user password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request
            ){
        User currentUser = getCurrentUser(authentication);
        log.info("Password change request for user: {}", currentUser.getId());
        userService.changePassword(currentUser.getId(), request);
        return okMessage("Password changed successfully");
    }
}
