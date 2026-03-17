package io.psyllabus.platform.platformadmin.dto.responses;

import io.psyllabus.platform.auth.enums.UserStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response body for {@code POST /api/platform/admins}.
 *
 * <p>Never includes the password — even as a hash. Raw password is a one-time
 * concern handled by the caller; this response confirms the account was created.</p>
 *
 * @param id        UUID of the created platform admin
 * @param username  username
 * @param email     email address
 * @param firstName first name
 * @param lastName  last name
 * @param status    account status (always {@code ACTIVE} on creation)
 * @param createdAt timestamp of account creation (UTC)
 */
public record PlatformAdminResponse(
        UUID id,
        String username,
        String email,
        String firstName,
        String lastName,
        UserStatus status,
        LocalDateTime createdAt
) {
}
