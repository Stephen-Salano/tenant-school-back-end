package io.psyllabus.platform.auth.controller;

import io.psyllabus.platform.auth.dto.request.LoginRequest;
import io.psyllabus.platform.auth.dto.responses.ClaimsResponse;
import io.psyllabus.platform.auth.dto.responses.LoginResponse;
import io.psyllabus.platform.auth.entities.User;
import io.psyllabus.platform.auth.services.AuthService;
import io.psyllabus.platform.auth.services.ClaimsService;
import io.psyllabus.platform.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final ClaimsService claimsService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Login successful"));
    }

    @GetMapping("/claims")
    public ResponseEntity<ApiResponse<ClaimsResponse>> getClaims(@AuthenticationPrincipal User user) {
        ClaimsResponse response = claimsService.getFullClaims(user.getId());
        return ResponseEntity.ok(ApiResponse.success(response, "Claims fetched successfully"));
    }
}
