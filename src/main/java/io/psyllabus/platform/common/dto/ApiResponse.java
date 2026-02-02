package io.psyllabus.platform.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;


@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse <T> (
    boolean success,
    String message,
    T data,
    LocalDateTime timestamp
)
{
    /**
     * Successful Response
     * @param data the data to be returned and used by generic type T
     * @param message to be displayed
     * @return new API response
     * @param <T> generic type to be inferred
     */
    public static <T> ApiResponse <T> success(T data, String message){
        return new ApiResponse<>(
                true,
                message,
                data,
                LocalDateTime.now()
        );
    }

    public static <T> ApiResponse<T> error(String message){
        return new ApiResponse<>(
                false,
                message,
                null,
                LocalDateTime.now()
        );
    }
}
