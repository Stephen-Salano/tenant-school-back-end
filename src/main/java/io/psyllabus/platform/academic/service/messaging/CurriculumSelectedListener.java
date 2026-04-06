package io.psyllabus.platform.academic.service.messaging;

import io.psyllabus.platform.academic.dto.events.CurriculumSelectedEvent;
import io.psyllabus.platform.academic.entities.AcademicTerm;
import io.psyllabus.platform.academic.entities.AcademicYear;
import io.psyllabus.platform.academic.repository.AcademicTermRepository;
import io.psyllabus.platform.academic.repository.AcademicYearRepository;
import io.psyllabus.platform.common.config.RabbitMQConfig;
import io.psyllabus.platform.common.util.TenantContext;
import io.psyllabus.platform.school.enums.CurriculumType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Listener for curriculum selection events.
 * Triggers automated drafting of academic terms based on the curriculum type.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CurriculumSelectedListener {

    private final AcademicYearRepository academicYearRepository;
    private final AcademicTermRepository academicTermRepository;

    /**
     * Responds to curriculum selection by auto-generating terms for the current/next academic year.
     * CBC generates 3 terms; IGCSE generates 2 terms.
     */
    @RabbitListener(queues = RabbitMQConfig.TERM_DRAFTING_QUEUE)
    @Transactional
    public void onCurriculumSelected(CurriculumSelectedEvent event) {
        log.info("Received curriculum selected event for tenant={}, type={}", 
                event.tenantId(), event.curriculumType());

        // We set the tenant context manually because the Rabbit worker runs in a separate thread
        TenantContext.setCurrentTenant(event.tenantId());
        try {
            processTermDrafting(event);
        } finally {
            TenantContext.clear();
        }
    }

    private void processTermDrafting(CurriculumSelectedEvent event) {
        // 1. Find or create the academic year for the given start year
        String yearName = String.valueOf(event.startYear());
        AcademicYear year = academicYearRepository.existsByNameAndTenantId(yearName, event.tenantId())
                ? academicYearRepository.findAllByTenantId(event.tenantId(), null)
                    .stream()
                    .filter(y -> y.getName().equals(yearName))
                    .findFirst()
                    .orElse(null)
                : createDefaultYear(event);

        if (year == null) {
            log.warn("Could not find or create academic year for drafting: {}", yearName);
            return;
        }

        // 2. Check if terms already exist for this year to avoid duplicates
        if (academicTermRepository.countByAcademicYearIdAndTenantId(year.getId(), event.tenantId()) > 0) {
            log.info("Terms already exist for year {}, skipping auto-drafting", yearName);
            return;
        }

        // 3. Generate draft terms based on curriculum type
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
            // Term 1: Jan - Apr
            terms.add(createTerm(year, "Term 1", 1, LocalDate.of(yearValue, 1, 6), LocalDate.of(yearValue, 4, 10)));
            // Term 2: May - Aug
            terms.add(createTerm(year, "Term 2", 2, LocalDate.of(yearValue, 5, 4), LocalDate.of(yearValue, 8, 7)));
            // Term 3: Sep - Dec
            terms.add(createTerm(year, "Term 3", 3, LocalDate.of(yearValue, 8, 31), LocalDate.of(yearValue, 11, 20)));
        } else if (type == CurriculumType.IGCSE) {
            // Term 1: Sep - Dec
            terms.add(createTerm(year, "Autumn Term", 1, LocalDate.of(yearValue, 9, 1), LocalDate.of(yearValue, 12, 18)));
            // Term 2: Jan - Jun
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
}
