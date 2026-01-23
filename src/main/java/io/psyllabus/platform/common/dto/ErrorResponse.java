package io.psyllabus.platform.common.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
/**
 * We use class here instead of java records becuase records are immutable by nature, we need flexibility which is
 * brought in by the @getters and @setters
 */
public class ErrorResponse {
    private String message;
    private String errorCode;
    private LocalDateTime timestamp;
    private Map<String, String> validationErrors;  // For field-specific errors

    public static ErrorResponse of(String message, String errorCode) {
        return ErrorResponse.builder()
                .message(message)
                .errorCode(errorCode)
                .timestamp(LocalDateTime.now())
                .build();
    }

}
