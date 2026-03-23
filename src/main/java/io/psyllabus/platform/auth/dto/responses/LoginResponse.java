package io.psyllabus.platform.auth.dto.responses;

import java.util.UUID;

/**
 * JSON body returned on successful login.
 *
 * token — short-lived JWT, stored in memory by the frontend.
 *
 * The refresh token is NOT here — it travels via the Set-Cookie response header
 * as an HttpOnly cookie, set by AuthController. This means JavaScript can never
 * read or steal the refresh token, even if an XSS attack occurs.
 */
public record LoginResponse(
  String accessToken,
  String tokenType,
  long expiresIn,
  UUID userId,
  String username,
  boolean passwordChangeRequired,
  String changeToken
) {}
