package io.psyllabus.platform.auth.dto.responses;

import io.psyllabus.platform.auth.enums.UserType;

import java.util.UUID;

public record UserResponse(
        UUID id,
        String username,
        String email,
        String firstName,
        String lastName,
        UserType userType
) {

}
