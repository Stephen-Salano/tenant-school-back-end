package io.psyllabus.platform.academic.service.impl;

import io.psyllabus.platform.academic.dto.events.CurriculumSelectedEvent;
import io.psyllabus.platform.academic.dto.request.AcademicTermCreateRequest;
import io.psyllabus.platform.academic.dto.request.AcademicTermUpdateRequest;
import io.psyllabus.platform.academic.dto.response.AcademicTermDetailResponse;
import io.psyllabus.platform.academic.dto.response.AcademicTermResponse;
import io.psyllabus.platform.academic.entities.AcademicTerm;
import io.psyllabus.platform.academic.entities.AcademicYear;
import io.psyllabus.platform.academic.repository.AcademicTermRepository;
import io.psyllabus.platform.academic.repository.AcademicYearRepository;
import io.psyllabus.platform.academic.service.AcademicTermService;
import io.psyllabus.platform.academic.service.mapper.AcademicTermMapper;
import io.psyllabus.platform.audit.enums.AuditAction;
import io.psyllabus.platform.audit.service.AuditService;
import io.psyllabus.platform.common.dto.PageResponse;
import io.psyllabus.platform.common.exception.DuplicateResourceException;
import io.psyllabus.platform.common.exception.ResourceNotFoundException;
import io.psyllabus.platform.common.exception.ValidationException;
import io.psyllabus.platform.common.util.TenantContext;
import io.psyllabus.platform.school.enums.CurriculumType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDate;
import java.util.ArrayList;
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

    public static final String ACADEMIC_TERM = "ACADEMIC_TERM";
    private final AcademicTermRepository academicTermRepository;
    private final AcademicYearRepository academicYearRepository;
    private final AcademicTermMapper academicTermMapper;
    private final AuditService auditService;

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

        auditService.log(AuditAction.ACADEMIC_SETUP, ACADEMIC_TERM, saved.getId(), "Created academic term: " + saved.getName());

        return academicTermMapper.toResponse(saved);
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
        return academicTermMapper.toResponse(term);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AcademicTermResponse> listByYear(UUID academicYearId) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        return academicTermRepository.findAllByAcademicYearIdAndTenantId(academicYearId, tenantId)
                .stream()
                .map(academicTermMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AcademicTermResponse> listAll(Pageable pageable) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        return PageResponse.from(
                academicTermRepository.findAllByTenantId(tenantId, pageable)
                        .map(academicTermMapper::toResponse)
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

        auditService.log(AuditAction.UPDATE, ACADEMIC_TERM, id, "Updated academic term details: " + saved.getName());

        return academicTermMapper.toResponse(saved);
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

        auditService.log(AuditAction.ACADEMIC_SETUP, ACADEMIC_TERM, id, "Set as current academic term: " + term.getName());

        return academicTermMapper.toResponse(saved);
    }

    /**
     * Responds to curriculum selection by auto-generating terms.
     * CBC generates 3 terms; IGCSE generates 2 terms.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCurriculumSelected(CurriculumSelectedEvent event) {
        log.info("Curriculum selection event received for tenant={}, type={}",
                event.tenantId(), event.curriculumType());

        // We set the tenant context manually because TransactionalEventListener
        // might run in a separate thread or context.
        TenantContext.setCurrentTenant(event.tenantId());
        try {
            autoGenerateTerms(event);
        } finally {
            TenantContext.clear();
        }
    }

    private void autoGenerateTerms(CurriculumSelectedEvent event) {
        String yearName = String.valueOf(event.startYear());

        // 1. Find or create the academic year
        AcademicYear year = academicYearRepository.findAllByTenantId(event.tenantId())
                .stream()
                .filter(y -> y.getName().equals(yearName))
                .findFirst()
                .orElseGet(() -> createDefaultYear(event));

        // 2. Avoid duplicates
        if (academicTermRepository.countByAcademicYearIdAndTenantId(year.getId(), event.tenantId()) > 0) {
            log.info("Terms already exist for year {}, skipping auto-drafting", yearName);
            return;
        }

        // 3. Generate draft terms
        List<AcademicTerm> terms = generateTerms(year, event.curriculumType());
        academicTermRepository.saveAll(terms);
        log.info("Auto-drafted {} terms for tenant={} year={}", terms.size(), event.tenantId(), yearName);
    }

    private AcademicYear createDefaultYear(CurriculumSelectedEvent event) {
        AcademicYear year = AcademicYear.builder()
                .tenantId(event.tenantId())
                .name(String.valueOf(event.startYear()))
                .startDate(LocalDate.of(event.startYear(), 1, 1))
                .endDate(LocalDate.of(event.startYear(), 12, 31))
                .isCurrent(false)
                .build();
        return academicYearRepository.save(year);
    }

    private List<AcademicTerm> generateTerms(AcademicYear year, CurriculumType type) {
        List<AcademicTerm> terms = new ArrayList<>();
        int yearValue = year.getStartDate().getYear();

        if (type == CurriculumType.CBC) {
            terms.add(createTerm(year, "Term 1", 1, LocalDate.of(yearValue, 1, 6), LocalDate.of(yearValue, 4, 10)));
            terms.add(createTerm(year, "Term 2", 2, LocalDate.of(yearValue, 5, 4), LocalDate.of(yearValue, 8, 7)));
            terms.add(createTerm(year, "Term 3", 3, LocalDate.of(yearValue, 8, 31), LocalDate.of(yearValue, 11, 20)));
        } else if (type == CurriculumType.IGCSE) {
            terms.add(createTerm(year, "Autumn Term", 1, LocalDate.of(yearValue, 9, 1), LocalDate.of(yearValue, 12, 18)));
            terms.add(createTerm(year, "Spring/Summer Term", 2, LocalDate.of(yearValue + 1, 1, 5), LocalDate.of(yearValue + 1, 6, 30)));
        }
        return terms;
    }

    private AcademicTerm createTerm(AcademicYear year, String name, int num, LocalDate start, LocalDate end) {
        return AcademicTerm.builder()
                .tenantId(year.getTenantId())
                .academicYearId(year.getId())
                .name(name)
                .termNumber(num)
                .startDate(start)
                .endDate(end)
                .isCurrent(false)
                .build();
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
