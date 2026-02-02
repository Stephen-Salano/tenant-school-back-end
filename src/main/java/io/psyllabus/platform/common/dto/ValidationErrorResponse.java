package io.psyllabus.platform.common.dto;

import java.time.LocalDateTime;
import java.util.Map;

public record ValidationErrorResponse(
        String message,
        Map<String, String> fieldErrors,
        LocalDateTime timestamp
) {
    /**
     * This returns wrapped field errors in Key, value pairing
     * @param fieldErrors form validation errors
     * @return new ValidationError response with the field and error associated
     */
    public static ValidationErrorResponse of(Map<String, String> fieldErrors){
        return  new ValidationErrorResponse(
                "Validation failed",
                fieldErrors,
                LocalDateTime.now()
        );
    }
}
