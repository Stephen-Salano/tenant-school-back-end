package io.psyllabus.platform.common.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Wraps paginated data from database queries
 * Instead of returning 150 students at once, you get page 1 of 8 (20 students per page).
 * @param content
 * @param page
 * @param size
 * @param totalElements
 * @param totalPages
 * @param last
 * @param <T>
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean last
) {
    public static <T> PageResponse<T> from(Page<T> page){
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }
}
