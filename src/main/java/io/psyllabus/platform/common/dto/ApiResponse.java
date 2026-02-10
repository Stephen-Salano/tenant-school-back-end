package io.psyllabus.platform.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;

/**
 * A standard generic API response wrapper.
 *
 * @param success   Indicates if the request was successful.
 * @param message   A descriptive message about the outcome.
 * @param data      The payload of the response.
 * @param errorDetails Detailed error information, if any.
 * @param timestamp The time the response was generated.
 * @param <T>       The type of the data payload.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse <T> (
    boolean success,
    String message,
    T data,
    ErrorResponse errorDetails, // New component to embed detailed error info
    LocalDateTime timestamp
)
{
    /**
     * Creates a standardized success response.
     *
     * @param data    The response payload.
     * @param message A success message.
     * @param <T>     The type of the payload.
     * @return A success ApiResponse instance.
     */
    public static <T> ApiResponse <T> success(T data, String message){
        return new ApiResponse<>(
                true,
                message,
                data,
                null, // No error details for success
                LocalDateTime.now()
        );
    }

    /**
     * Creates a standardized simple error response.
     *
     * @param message The error message.
     * @param <T>     The type of the payload (will be null).
     * @return A simple error ApiResponse instance.
     */
    public static <T> ApiResponse<T> error(String message){
        return new ApiResponse<>(
                false,
                message,
                null,
                null, // No specific error details for simple error
                LocalDateTime.now()
        );
    }

    /**
     * Creates a standardized detailed error response.
     *
     * @param message The error message.
     * @param errorDetails Detailed error information.
     * @param <T>     The type of the payload (will be null).
     * @return A detailed error ApiResponse instance.
     */
    public static <T> ApiResponse<T> error(String message, ErrorResponse errorDetails){
        return new ApiResponse<>(
                false,
                message,
                null,
                errorDetails, // Include error details
                LocalDateTime.now()
        );
    }
}
