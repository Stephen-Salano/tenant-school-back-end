package io.psyllabus.platform.auth.dto.responses;

import java.util.UUID;

public record LoginResponse(
  String token,
  String tokenType,
  long expiresIn,
  UUID userId,
  String username
) {}
