package io.psyllabus.platform.attendance.repository;

import io.psyllabus.platform.attendance.dto.request.AttendanceSearchFilterRequest;
import io.psyllabus.platform.attendance.entities.AttendanceRecord;
import io.psyllabus.platform.student.entities.Student;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Subquery;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class AttendanceSpecifications {

    public static final String TENANT_ID = "tenantId";
    public static final String STUDENT_ID = "studentId";

    private AttendanceSpecifications() {
    }

    public static Specification<AttendanceRecord> withFilters(AttendanceSearchFilterRequest filter) {
        return hasTenant(filter.tenantId())
                .and(hasStudentIds(filter.studentIds()))
                .and(hasStudentId(filter.studentId()))
                .and(hasClassId(filter.classId()))
                .and(hasAcademicTermId(filter.academicTermId()))
                .and(hasExactDate(filter.date()))
                .and(hasDateRange(filter.startDate(), filter.endDate()))
                .and(hasStatus(filter.status()))
                .and(hasStudentName(filter.studentName()));
    }

    private static Specification<AttendanceRecord> hasTenant(UUID tenantId) {
        return (root, query, cb) -> cb.equal(root.get(TENANT_ID), tenantId);
    }

    private static Specification<AttendanceRecord> hasStudentIds(List<UUID> studentIds) {
        if (studentIds == null || studentIds.isEmpty()) {
            return null;
        }
        return (root, query, cb) -> root.get(STUDENT_ID).in(studentIds);
    }

    private static Specification<AttendanceRecord> hasStudentId(UUID studentId) {
        if (studentId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get(STUDENT_ID), studentId);
    }

    private static Specification<AttendanceRecord> hasClassId(UUID classId) {
        if (classId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("classId"), classId);
    }

    private static Specification<AttendanceRecord> hasAcademicTermId(UUID academicTermId) {
        if (academicTermId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("academicTermId"), academicTermId);
    }

    private static Specification<AttendanceRecord> hasExactDate(java.time.LocalDate date) {
        if (date == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("date"), date);
    }

    private static Specification<AttendanceRecord> hasDateRange(java.time.LocalDate startDate, java.time.LocalDate endDate) {
        if (startDate == null && endDate == null) {
            return null;
        }
        return (root, query, cb) -> {
            if (startDate != null && endDate != null) {
                return cb.between(root.get("date"), startDate, endDate);
            }
            if (startDate != null) {
                return cb.greaterThanOrEqualTo(root.get("date"), startDate);
            }
            return cb.lessThanOrEqualTo(root.get("date"), endDate);
        };
    }

    private static Specification<AttendanceRecord> hasStatus(io.psyllabus.platform.attendance.enums.AttendanceStatus status) {
        if (status == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    private static Specification<AttendanceRecord> hasStudentName(String studentName) {
        if (studentName == null || studentName.isBlank()) {
            return null;
        }

        return (root, query, cb) -> {
            assert query != null;
            query.distinct(true);
            String likeValue = "%" + studentName.trim().toLowerCase(Locale.ROOT) + "%";

            Subquery<UUID> studentSubquery = query.subquery(UUID.class);
            var studentRoot = studentSubquery.from(Student.class);
            var fullName = cb.lower(
                    cb.concat(
                            cb.concat(studentRoot.get("firstName"), " "),
                            studentRoot.get("lastName")
                    )
            );

            studentSubquery.select(studentRoot.get("id"))
                    .where(
                            cb.equal(studentRoot.get(TENANT_ID), root.get(TENANT_ID)),
                            cb.or(
                                    cb.like(cb.lower(studentRoot.get("firstName")), likeValue),
                                    cb.like(cb.lower(studentRoot.get("lastName")), likeValue),
                                    cb.like(fullName, likeValue),
                                    cb.like(cb.lower(studentRoot.get("admissionNumber")), likeValue)
                            )
                    );

            return root.get(STUDENT_ID).in(studentSubquery);
        };
    }
}
