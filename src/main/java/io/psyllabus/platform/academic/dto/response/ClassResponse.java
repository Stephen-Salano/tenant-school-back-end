package io.psyllabus.platform.academic.dto.response;

import io.psyllabus.platform.academic.enums.AcademicStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Lightweight class response — used in list endpoints.
 * Does not include enrollment counts; see {@link ClassDetailResponse} for richer single-fetch output.
 */
public record ClassResponse(
    UUID id,
    String name,
    String gradeLevel,
    String streamName,
    UUID academicYearId,
    UUID branchId,
    Integer maxCapacity,
    AcademicStatus status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
