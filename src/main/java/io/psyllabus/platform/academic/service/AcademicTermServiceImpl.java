package io.psyllabus.platform.academic.service;

import io.psyllabus.platform.academic.dto.request.AcademicTermCreateRequest;
import io.psyllabus.platform.academic.dto.request.AcademicTermUpdateRequest;
import io.psyllabus.platform.academic.dto.response.AcademicTermDetailResponse;
import io.psyllabus.platform.academic.dto.response.AcademicTermResponse;
import io.psyllabus.platform.academic.entities.AcademicTerm;
import io.psyllabus.platform.academic.entities.AcademicYear;
import io.psyllabus.platform.academic.repository.AcademicTermRepository;
import io.psyllabus.platform.academic.repository.AcademicYearRepository;
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
import java.util.List;
import java.util.UUID;

/**
 * Implementation of {@link AcademicTermService}.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class AcademicTermServiceImpl implements AcademicTermService {

    private final AcademicTermRepository academicTermRepository;
    private final AcademicYearRepository academicYearRepository;

    @Override
    public AcademicTermResponse create(AcademicTermCreateRequest request) {
        UUID tenantId = TenantContext.getCurrentTenantId();

        AcademicYear year = findYearByIdAndTenant(request.academicYearId(), tenantId);
        validateDates(request.startDate(), request.endDate(), year);

        if (academicTermRepository.existsByAcademicYearIdAndTermNumberAndTenantId(
                request.academicYearId(), request.termNumber(), tenantId)) {
            throw new DuplicateResourceException("Term number " + request.termNumber() + 
                    " already exists for this academic year");
        }

        AcademicTerm term = AcademicTerm.builder()
                .tenantId(tenantId)
                .academicYearId(request.academicYearId())
                .name(request.name())
                .termNumber(request.termNumber())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .isCurrent(false)
                .build();

        AcademicTerm saved = academicTermRepository.save(term);
        log.info("Academic term created: id={} name={} tenant={}", saved.getId(), saved.getName(), tenantId);
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public AcademicTermDetailResponse getById(UUID id) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        AcademicTerm term = findByIdAndTenant(id, tenantId);
        AcademicYear year = findYearByIdAndTenant(term.getAcademicYearId(), tenantId);
        return mapToDetailResponse(term, year.getName());
    }

    @Override
    @Transactional(readOnly = true)
    public AcademicTermResponse getCurrentTerm() {
        UUID tenantId = TenantContext.getCurrentTenantId();
        AcademicTerm term = academicTermRepository.findByTenantIdAndIsCurrentTrue(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("No current academic term set for this school"));
        return mapToResponse(term);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AcademicTermResponse> listByYear(UUID academicYearId) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        return academicTermRepository.findAllByAcademicYearIdAndTenantId(academicYearId, tenantId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AcademicTermResponse> listAll(Pageable pageable) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        return PageResponse.from(
                academicTermRepository.findAllByTenantId(tenantId, pageable)
                        .map(this::mapToResponse)
        );
    }

    @Override
    public AcademicTermResponse update(UUID id, AcademicTermUpdateRequest request) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        AcademicTerm term = findByIdAndTenant(id, tenantId);
        AcademicYear year = findYearByIdAndTenant(term.getAcademicYearId(), tenantId);

        if (request.termNumber() != null && request.termNumber() != term.getTermNumber()) {
            if (academicTermRepository.existsByAcademicYearIdAndTermNumberAndTenantId(
                    term.getAcademicYearId(), request.termNumber(), tenantId)) {
                throw new DuplicateResourceException("Term number " + request.termNumber() + 
                        " already exists for this academic year");
            }
            term.setTermNumber(request.termNumber());
        }

        if (request.name() != null) term.setName(request.name());

        LocalDate effectiveStart = request.startDate() != null ? request.startDate() : term.getStartDate();
        LocalDate effectiveEnd = request.endDate() != null ? request.endDate() : term.getEndDate();
        validateDates(effectiveStart, effectiveEnd, year);

        if (request.startDate() != null) term.setStartDate(request.startDate());
        if (request.endDate() != null) term.setEndDate(request.endDate());

        AcademicTerm saved = academicTermRepository.save(term);
        log.info("Academic term updated: id={} tenant={}", id, tenantId);
        return mapToResponse(saved);
    }

    @Override
    public AcademicTermResponse setCurrentTerm(UUID id) {
        UUID tenantId = TenantContext.getCurrentTenantId();

        academicTermRepository.findByTenantIdAndIsCurrentTrue(tenantId)
                .ifPresent(existing -> {
                    existing.setCurrent(false);
                    academicTermRepository.save(existing);
                    log.debug("Cleared current term flag: id={} name={}", existing.getId(), existing.getName());
                });

        AcademicTerm term = findByIdAndTenant(id, tenantId);
        term.setCurrent(true);
        AcademicTerm saved = academicTermRepository.save(term);

        log.info("Current academic term set: id={} name={} tenant={}", id, term.getName(), tenantId);
        return mapToResponse(saved);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private AcademicTerm findByIdAndTenant(UUID id, UUID tenantId) {
        return academicTermRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Academic term not found: " + id));
    }

    private AcademicYear findYearByIdAndTenant(UUID id, UUID tenantId) {
        return academicYearRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Academic year not found: " + id));
    }

    private void validateDates(LocalDate start, LocalDate end, AcademicYear year) {
        if (!start.isBefore(end)) {
            throw new ValidationException("Start date must be before end date");
        }
        if (start.isBefore(year.getStartDate()) || end.isAfter(year.getEndDate())) {
            throw new ValidationException("Term dates must be within the academic year range: " + 
                    year.getStartDate() + " to " + year.getEndDate());
        }
    }

    private AcademicTermResponse mapToResponse(AcademicTerm term) {
        return new AcademicTermResponse(
                term.getId(),
                term.getAcademicYearId(),
                term.getName(),
                term.getTermNumber(),
                term.getStartDate(),
                term.getEndDate(),
                term.isCurrent(),
                term.getCreatedAt(),
                term.getUpdatedAt()
        );
    }

    private AcademicTermDetailResponse mapToDetailResponse(AcademicTerm term, String yearName) {
        return new AcademicTermDetailResponse(
                term.getId(),
                term.getAcademicYearId(),
                yearName,
                term.getName(),
                term.getTermNumber(),
                term.getStartDate(),
                term.getEndDate(),
                term.isCurrent(),
                term.getCreatedAt(),
                term.getUpdatedAt()
        );
    }
}
