package io.psyllabus.platform.auth.dto.responses;

/**
 * Returned by POST /api/auth/refresh.
 *
 * Only contains the new access token — the refresh token stays
 * in the HttpOnly cookie untouched (no rotation at MVP stage).
 * expiresIn tells the frontend when to schedule the next refresh.
 */
public record RefreshResponse(
        String accessToken,
        String tokenType,
        long expiresIn
) {
    public static RefreshResponse of(String accessToken, long expiresIn) {
        return new RefreshResponse(accessToken, "Bearer", expiresIn);
    }
}
