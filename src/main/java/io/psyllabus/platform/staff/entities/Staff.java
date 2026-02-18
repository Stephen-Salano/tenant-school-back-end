package io.psyllabus.platform.staff.entities;

import io.psyllabus.platform.common.audit.Auditable;
import io.psyllabus.platform.staff.enums.EmploymentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Staff = The person (John Kamau, employee #123)
 */
@Entity
@Table(name = "staff", indexes = {
        @Index(name = "idx_staff_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_staff_user_id", columnList = "user_id"),
        @Index(name = "idx_staff_employee_number", columnList = "employee_number,tenant_id", unique = true),
        @Index(name = "idx_staff_department", columnList = "department_id")
})
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class Staff extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "employee_number", nullable = false, length = 50, unique = true)
    private String employeeNumber;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "department_id")
    private UUID departmentId;

    @Column(name = "hire_date", nullable = false)
    private LocalDate hireDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "employment_status", length = 20)
    @Builder.Default
    private EmploymentStatus employmentStatus = EmploymentStatus.ACTIVE;

    @Column(name = "qualification", length = 255)
    private String qualification;

    @Column(name = "specialization", columnDefinition = "TEXT")
    private String specialization;
}
