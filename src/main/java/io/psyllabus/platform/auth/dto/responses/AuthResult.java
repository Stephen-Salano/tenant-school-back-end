package io.psyllabus.platform.auth.dto.responses;


/**
 * Internal result from AuthService.login().
 *
 * Why not include refreshToken in LoginResponse?
 * LoginResponse is the JSON body — it should never carry the refresh token.
 * The refresh token must go into an HttpOnly cookie, set by the controller.
 *
 * This record gives AuthController both values cleanly:
 *   authResult.loginResponse()  → serialized to JSON response body
 *   authResult.refreshToken()   → set as HttpOnly cookie on HttpServletResponse
 *
 * The controller is the only consumer of refreshToken() here.
 * The service layer has no knowledge of HTTP cookies.
 */
public record  AuthResult(
        LoginResponse loginResponse,
        String refreshToken // full refresh JWT - controller puts this in HttpOnly cookie
) {
}
