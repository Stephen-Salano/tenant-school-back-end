package io.psyllabus.platform.common.exception;

import io.psyllabus.platform.common.dto.ApiResponse;
import io.psyllabus.platform.common.dto.ErrorResponse;
import io.psyllabus.platform.common.dto.ValidationErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Shared between multiple controllers
 * @RestControllerAdvice: REST controller methods return value should be rendered into the response body
 */

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler  {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFound(ResourceNotFoundException ex){
        log.warn("Resource not found: {}", ex.getMessage());
        ErrorResponse errorDetails = ErrorResponse.of(ex.getMessage(), "RESOURCE_NOT_FOUND");
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage(), errorDetails));
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnauthorized(UnauthorizedException ex){
        log.warn("Unauthorized access: {}", ex.getMessage());
        ErrorResponse errorDetails = ErrorResponse.of(ex.getMessage(), "UNAUTHORIZED");
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(ex.getMessage(), errorDetails));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidCredentials(InvalidCredentialsException ex){
        log.warn("Invalid credentials: {}", ex.getMessage());
        ErrorResponse errorDetails = ErrorResponse.of(ex.getMessage(), "INVALID_CREDENTIALS");
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(ex.getMessage(), errorDetails));
    }

    @ExceptionHandler(TenantNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleTenantNotFound(TenantNotFoundException ex){
        log.error("Tenant not found: {}", ex.getMessage());
        ErrorResponse errorDetails = ErrorResponse.of(ex.getMessage(), "TENANT_NOT_FOUND");
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage(), errorDetails));
    }

    @ExceptionHandler(SubscriptionException.class)
    public ResponseEntity<ApiResponse<Void>> handleSubscriptionError(SubscriptionException ex){
        log.warn("Subscription error: {}", ex.getMessage());
        ErrorResponse errorDetails = ErrorResponse.of(ex.getMessage(), "SUBSCRIPTION_ERROR");
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(ex.getMessage(), errorDetails));
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicateResource(DuplicateResourceException ex){
        ErrorResponse errorDetails = ErrorResponse.of(ex.getMessage(), "DUPLICATE_RESOURCE");
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage(), errorDetails));

    }

    // 400
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(ValidationException ex){
        log.warn("Validation error: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage(), ErrorResponse.of(ex.getMessage(), "VALIDATION_ERROR")));
    }

    // 403
    @ExceptionHandler(InsufficientPermissionException.class)
    public ResponseEntity<ApiResponse<Void>> handlePermission(InsufficientPermissionException ex){
        log.warn("Permission denied: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(ex.getMessage(), ErrorResponse.of(ex.getMessage(), "PERMISSION_DENIED")));
    }




    /**
     * “If a MethodArgumentNotValidException happens, call the method below.”
     * That exception is thrown when:
     * Spring receives a request
     * Tries to put the request data into a Java object
     * Validation fails (@NotNull, @Email, etc.)
     * Instead of crashing the app or returning a generic error, Spring routes the error here.
     * @param ex the exception being thrwown
     * @return A detailed list of all field errors
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex){
        Map<String, String> errors = new HashMap<>();
        /**
         * getBindingResult() → the validation report
         * getAllErrors() → list of all problems (field-level and object-level)
         * forEach → process them one by one
         */
        ex.getBindingResult().getAllErrors().forEach(error -> {
            /**
             * error is a generic validation error
             * You cast it to FieldError because field validations (@NotNull, etc.) produce field errors
             * getField() returns the field name as a string
             */
            String fieldName = ((FieldError) error).getField();
            // This fetches the human-readable message.
            String errorMessage = error.getDefaultMessage();
            // This stores the error in the map.
            errors.put(fieldName, errorMessage);
        });

        log.warn("Validation failed:{}", errors);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ValidationErrorResponse.of(errors));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex){
        log.error("Unexpected error", ex);
        ErrorResponse errorDetails = ErrorResponse.of("An unexpected error occurred", "INTERNAL_ERROR");
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("An unexpected error occurred", errorDetails));
    }
}
