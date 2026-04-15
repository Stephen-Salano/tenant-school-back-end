package io.psyllabus.platform.academic.service.impl;

import io.psyllabus.platform.academic.dto.request.AcademicYearCreateRequest;
import io.psyllabus.platform.academic.dto.request.AcademicYearUpdateRequest;
import io.psyllabus.platform.academic.dto.response.AcademicYearDetailResponse;
import io.psyllabus.platform.academic.dto.response.AcademicYearResponse;
import io.psyllabus.platform.academic.entities.AcademicYear;
import io.psyllabus.platform.academic.repository.AcademicTermRepository;
import io.psyllabus.platform.academic.repository.AcademicYearRepository;
import io.psyllabus.platform.academic.service.AcademicYearService;
import io.psyllabus.platform.academic.service.mapper.AcademicYearMapper;
import io.psyllabus.platform.audit.enums.AuditAction;
import io.psyllabus.platform.audit.service.AuditService;
import io.psyllabus.platform.common.dto.PageResponse;
import io.psyllabus.platform.common.exception.DuplicateResourceException;
import io.psyllabus.platform.common.exception.ResourceNotFoundException;
import io.psyllabus.platform.common.exception.ValidationException;
import io.psyllabus.platform.common.util.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Implementation of {@link AcademicYearService}.
 *
 * <p><strong>Optimistic locking note on {@link #setCurrentYear(UUID)}:</strong>
 * Both the departing year (cleared) and the incoming year (activated) are updated
 * in a single {@code @Transactional} scope. The {@code @Version} field on
 * {@link AcademicYear} means Hibernate appends {@code WHERE version = ?} to each
 * UPDATE. If a concurrent request modifies either row between our read and our
 * write, Hibernate throws {@code ObjectOptimisticLockingFailureException} and the
 * whole transaction rolls back — neither year's {@code isCurrent} flag is changed.
 * The caller retries.</p>
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class AcademicYearServiceImpl implements AcademicYearService {

    public static final String ACADEMIC_YEAR = "ACADEMIC_YEAR";
    private final AcademicYearRepository academicYearRepository;
    private final AcademicTermRepository academicTermRepository;
    private final AcademicYearMapper academicYearMapper;
    private final AuditService auditService;

    // ── Create ────────────────────────────────────────────────────────────────

    @Override
    public AcademicYearResponse create(AcademicYearCreateRequest request) {
        UUID tenantId = TenantContext.getCurrentTenantId();

        validateDates(request.startDate(), request.endDate());

        if (academicYearRepository.existsByNameAndTenantId(request.name(), tenantId)) {
            throw new DuplicateResourceException("Academic year already exists: " + request.name());
        }

        AcademicYear year = AcademicYear.builder()
                .tenantId(tenantId)
                .name(request.name())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .isCurrent(false)
                .build();

        AcademicYear saved = academicYearRepository.save(year);
        log.info("Academic year created: id={} name={} tenant={}", saved.getId(), saved.getName(), tenantId);

        auditService.log(AuditAction.ACADEMIC_SETUP, ACADEMIC_YEAR, saved.getId(), "Created academic year: " + saved.getName());

        return academicYearMapper.toResponse(saved);
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public AcademicYearDetailResponse getById(UUID id) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        AcademicYear year = findByIdAndTenant(id, tenantId);
        return mapToDetailResponse(year);
    }

    @Override
    @Transactional(readOnly = true)
    public AcademicYearResponse getCurrentYear() {
        UUID tenantId = TenantContext.getCurrentTenantId();
        AcademicYear year = academicYearRepository.findByTenantIdAndIsCurrentTrue(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("No current academic year set for this school"));
        return academicYearMapper.toResponse(year);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AcademicYearResponse> listAll(Pageable pageable) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        return PageResponse.from(
                academicYearRepository.findAllByTenantId(tenantId, pageable)
                        .map(academicYearMapper::toResponse)
        );
    }

    // ── Update ────────────────────────────────────────────────────────────────

    /**
     * Sets a new current academic year for the tenant.
     *
     * <p>Flow:
     * <ol>
     *   <li>Clear {@code isCurrent} on the existing current year (if any) — version increments.</li>
     *   <li>Set {@code isCurrent} on the target year — version increments.</li>
     *   <li>Both happen in one transaction. A concurrent modification on either row
     *       causes {@code ObjectOptimisticLockingFailureException} and a full rollback.</li>
     * </ol>
     * </p>
     */
    @Override
    public AcademicYearResponse setCurrentYear(UUID yearId) {
        UUID tenantId = TenantContext.getCurrentTenantId();

        // Step 1: Clear the existing current year (if any)
        academicYearRepository.findByTenantIdAndIsCurrentTrue(tenantId)
                .ifPresent(existing -> {
                    existing.setCurrent(false);
                    academicYearRepository.save(existing);
                    log.debug("Cleared current flag: id={} name={}", existing.getId(), existing.getName());
                });

        // Step 2: Activate the requested year
        AcademicYear year = findByIdAndTenant(yearId, tenantId);
        year.setCurrent(true);
        AcademicYear saved = academicYearRepository.save(year);

        log.info("Current academic year set: id={} name={} tenant={}", yearId, year.getName(), tenantId);

        auditService.log(AuditAction.ACADEMIC_SETUP, ACADEMIC_YEAR, yearId, "Set as current academic year: " + year.getName());

        return academicYearMapper.toResponse(saved);
    }

    @Override
    public AcademicYearResponse update(UUID id, AcademicYearUpdateRequest request) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        AcademicYear year = findByIdAndTenant(id, tenantId);

        // Apply PATCH semantics — only non-null fields overwrite existing values
        if (request.name() != null) {
            boolean nameChanged = !request.name().equalsIgnoreCase(year.getName());
            if (nameChanged && academicYearRepository.existsByNameAndTenantId(request.name(), tenantId)) {
                throw new DuplicateResourceException("Academic year already exists: " + request.name());
            }
            year.setName(request.name());
        }

        // Validate resulting dates using effective values (existing or incoming)
        LocalDate effectiveStart = request.startDate() != null ? request.startDate() : year.getStartDate();
        LocalDate effectiveEnd   = request.endDate()   != null ? request.endDate()   : year.getEndDate();
        validateDates(effectiveStart, effectiveEnd);

        if (request.startDate() != null) year.setStartDate(request.startDate());
        if (request.endDate()   != null) year.setEndDate(request.endDate());

        AcademicYear saved = academicYearRepository.save(year);
        log.info("Academic year updated: id={} tenant={}", id, tenantId);

        auditService.log(AuditAction.UPDATE, ACADEMIC_YEAR, id, "Updated academic year details: " + saved.getName());

        return academicYearMapper.toResponse(saved);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private AcademicYear findByIdAndTenant(UUID id, UUID tenantId) {
        return academicYearRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Academic year not found: " + id));
    }

    /**
     * Validates that {@code start} is strictly before {@code end}.
     * Called on create and on update to guard the effective date range.
     */
    private void validateDates(LocalDate start, LocalDate end) {
        if (!start.isBefore(end)) {
            throw new ValidationException("Start date must be before end date");
        }
    }

    private AcademicYearDetailResponse mapToDetailResponse(AcademicYear year) {
        long termCount = academicTermRepository.countByAcademicYearIdAndTenantId(year.getId(), year.getTenantId());
        return new AcademicYearDetailResponse(
                year.getId(),
                year.getName(),
                year.getStartDate(),
                year.getEndDate(),
                year.isCurrent(),
                (int) termCount,
                year.getCreatedAt(),
                year.getUpdatedAt()
        );
    }
}
