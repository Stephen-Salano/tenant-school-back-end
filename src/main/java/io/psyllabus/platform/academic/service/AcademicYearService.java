package io.psyllabus.platform.academic.service;

import io.psyllabus.platform.academic.dto.request.AcademicYearCreateRequest;
import io.psyllabus.platform.academic.dto.request.AcademicYearUpdateRequest;
import io.psyllabus.platform.academic.dto.response.AcademicYearDetailResponse;
import io.psyllabus.platform.academic.dto.response.AcademicYearResponse;
import io.psyllabus.platform.common.dto.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Manages the top-level academic calendar for a tenant.
 *
 * <p>A tenant has exactly one current academic year at any time. {@link #setCurrentYear(UUID)}
 * is guarded by optimistic locking ({@code @Version} on the entity) to prevent two concurrent
 * admin sessions from setting two different years as current simultaneously — only the first
 * write wins.</p>
 */
public interface AcademicYearService {

    /**
     * Create a new academic year for the current tenant.
     * Name must be unique within the tenant. Start date must be before end date.
     *
     * @param request creation details
     * @return the created academic year
     * @throws io.psyllabus.platform.common.exception.DuplicateResourceException if the name already exists
     * @throws io.psyllabus.platform.common.exception.ValidationException if dates are invalid
     */
    AcademicYearResponse create(AcademicYearCreateRequest request);

    /**
     * Fetch a single academic year by ID, tenant-scoped.
     *
     * @param id the academic year UUID
     * @return the academic year detail including term count
     * @throws io.psyllabus.platform.common.exception.ResourceNotFoundException if not found
     */
    AcademicYearDetailResponse getById(UUID id);

    /**
     * Fetch the currently active academic year for this tenant.
     *
     * @return the current academic year
     * @throws io.psyllabus.platform.common.exception.ResourceNotFoundException if no year is set as current
     */
    AcademicYearResponse getCurrentYear();

    /**
     * Mark a specific academic year as current, clearing the previous one.
     *
     * <p><strong>Concurrency guard:</strong> both the departing and incoming year
     * rows carry a {@code @Version} counter. If a concurrent request modifies either
     * between our read and our write, Spring raises
     * {@code ObjectOptimisticLockingFailureException}, which the
     * {@code GlobalExceptionHandler} maps to HTTP 409.</p>
     *
     * @param yearId the academic year to make current
     * @return the updated academic year
     * @throws io.psyllabus.platform.common.exception.ResourceNotFoundException if the year is not found
     */
    AcademicYearResponse setCurrentYear(UUID yearId);

    /**
     * Partial update of an academic year (PATCH semantics).
     * Null fields in the request are ignored.
     *
     * @param id      the academic year UUID
     * @param request fields to update
     * @return the updated academic year
     * @throws io.psyllabus.platform.common.exception.ResourceNotFoundException if not found
     * @throws io.psyllabus.platform.common.exception.ValidationException if resulting dates are invalid
     */
    AcademicYearResponse update(UUID id, AcademicYearUpdateRequest request);

    /**
     * Paginated list of all academic years for the current tenant.
     *
     * @param pageable pagination and sort parameters
     * @return page of academic years
     */
    PageResponse<AcademicYearResponse> listAll(Pageable pageable);
}
