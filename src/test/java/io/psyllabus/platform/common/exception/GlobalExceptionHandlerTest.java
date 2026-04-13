package io.psyllabus.platform.common.exception;

import io.psyllabus.platform.common.dto.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {

    @Test
    void handleDataIntegrity_returnsConflictWithoutExposingDatabaseMessageInPayload() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        DataIntegrityViolationException ex = new DataIntegrityViolationException(
                "constraint violation",
                new RuntimeException("Key (email)=(john@example.com) already exists.")
        );

        ResponseEntity<ApiResponse<Void>> response = handler.handleDataIntegrity(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(false, response.getBody().success());
        assertEquals(
                "A record with this identifier already exists. This may indicate a duplicate entry or a conflicting assignment.",
                response.getBody().message()
        );
        assertEquals(
                "DATA_INTEGRITY_VIOLATION",
                response.getBody().errorDetails().getErrorCode()
        );
    }
}
