package io.psyllabus.platform.common.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Shared constants for the Authentication and Authorization domain.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AuthConstants {

    /** Prefix for password change tokens stored in Redis. */
    public static final String CHANGE_TOKEN_PREFIX = "change_token:";

    /** Constant UUID used for platform-level records that require a tenantId (e.g. RefreshTokens for PlatformAdmins). */
    public static final java.util.UUID SYSTEM_TENANT_ID = java.util.UUID.fromString("00000000-0000-0000-0000-000000000000");

}
