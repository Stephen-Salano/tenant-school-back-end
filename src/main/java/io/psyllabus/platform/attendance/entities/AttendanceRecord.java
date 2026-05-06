package io.psyllabus.platform.attendance.entities;

import io.psyllabus.platform.attendance.enums.AttendanceStatus;
import io.psyllabus.platform.common.audit.Auditable;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(
        name = "attendance_records",
        indexes = {
                @Index(name = "idx_attendance_tenant", columnList = "tenant_id"),
                @Index(name = "idx_attendance_class_date", columnList = "tenant_id,class_id,date"),
                @Index(name = "idx_attendance_student_date", columnList = "tenant_id,student_id,date"),
                @Index(name = "idx_attendance_term", columnList = "tenant_id,academic_term_id"),
                @Index(name = "idx_attendance_idempotency", columnList = "tenant_id,idempotency_key")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_attendance_tenant_class_student_date",
                        columnNames = {"tenant_id", "class_id", "student_id", "date"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceRecord extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "class_id", nullable = false)
    private UUID classId;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AttendanceStatus status;

    @Column(name = "marked_by", nullable = false)
    private UUID markedBy;

    @Column(name = "academic_term_id", nullable = false)
    private UUID academicTermId;

    @Column(name = "idempotency_key", nullable = false, length = 120)
    private String idempotencyKey;
}
