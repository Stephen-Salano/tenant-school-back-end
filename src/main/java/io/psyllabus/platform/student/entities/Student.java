package io.psyllabus.platform.student.entities;

import io.psyllabus.platform.common.audit.Auditable;
import io.psyllabus.platform.student.enums.Gender;
import io.psyllabus.platform.student.enums.StudentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "students", indexes = {
        @Index(name = "idx_student_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_student_user_id", columnList = "user_id"),
        @Index(name = "idx_student_admission", columnList = "admission_number,tenant_id", unique = true),
        @Index(name = "idx_student_class", columnList = "current_class_id")
})
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter

public class Student extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "admission_number", nullable = false, length = 50)
    private String admissionNumber;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", length = 10)
    private Gender gender;

    @Column(name = "current_class_id")
    private UUID currentClassId;

    @Column(name = "addmission_date", nullable = false)
    private LocalDate admissionDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_status", length = 20)
    @Builder.Default
    private StudentStatus currentStatus = StudentStatus.ACTIVE;

    @Column(name = "medical_conditions", columnDefinition = "TEXT")
    private String medicalConditions;

    @Column(name = "photo_url", columnDefinition = "TEXT")
    private String photoUrl;

}
