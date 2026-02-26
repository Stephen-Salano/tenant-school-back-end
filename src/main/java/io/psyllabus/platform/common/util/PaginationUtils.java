package io.psyllabus.platform.common.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Pagination helpers — controllers use these to build Pageable from query params.
 *
 * Usage:
 *   Pageable pageable = PaginationUtils.of(page, size, "createdAt");
 *   Page<Staff> results = staffRepository.findByTenantId(tenantId, pageable);
 *   return PageResponse.from(results.map(this::mapToResponse));
 */

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PaginationUtils {

    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    /**
     * Building a descending-sorted Pageable (newest first - good default for lists)
     * @param page the actual page number
     * @param size how many items per page
     * @param sortBy name of the column to sort by
     * @return a Pageable object
     */
    public static Pageable of(int page, int size, String sortBy){
        // prevens errors incase someone requests page 5, it resets to 0
        int safePage = Math.max(0, page);
        // Keeps the page size reasonable, ensures the size is at least 1, but no longer than a predefined MAX_SIZE
        // It prevents the system from crashing in-case a resource requests a million items at once
        int safeSize = Math.clamp(size, 1, MAX_SIZE);
        // combines the safe inputs into a single "request" object.
        // It specifically tells the db to sort results in descending order (DES) using the sortBy column name provided
        return PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, sortBy));
    }

    /**
     * Build Pageable with explicit sort direction
     * @param page the actual page number
     * @param size how many items per page
     * @param sortBy name of the column to sort by
     * @param direction order of sorting
     * @return a pageable object / a page
     */
    public static Pageable of(int page, int size, String sortBy, Sort.Direction direction){
        int safePage = Math.max(0, page);
        int safeSize = Math.clamp(size, 1, MAX_SIZE );
        return PageRequest.of(safePage, safeSize, Sort.by(direction, sortBy));
    }

    /**
     * Default Pageable - page 0, 20 items, sorted by createdAt desc
     * @return a  pageable object
     */
    public static Pageable defaults(){
        return of(DEFAULT_PAGE, DEFAULT_SIZE, "createdAT");
    }


}
