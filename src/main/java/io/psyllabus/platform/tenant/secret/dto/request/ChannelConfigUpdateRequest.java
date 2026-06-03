package io.psyllabus.platform.tenant.secret.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record ChannelConfigUpdateRequest(
    Boolean emailEnabled,
    Boolean smsEnabled,
    @Size(max = 100) String emailFromName,
    @Email @Size(max = 100) String emailReplyTo
) {}
