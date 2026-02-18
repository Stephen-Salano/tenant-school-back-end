package io.psyllabus.platform.guardian.entities;

import io.psyllabus.platform.common.audit.Auditable;
import io.psyllabus.platform.guardian.enums.RelationshipType;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * This is a Join table for Students and Guardians to avoid heavy DB calls
 */
@Entity
@Table(name = "student_guardians", indexes = {
        @Index(name = "idx_student_guardian_student", columnList = "student_id"),
        @Index(name = "idx_student_guardian_guardian", columnList = "guardian_id"),
        @Index(name = "idx_student_guardian_tenant", columnList = "tenant_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_student_guardian", columnNames =  {"student_id", "guardian_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentGuardian extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "guardian_id", nullable = false)
    private UUID guardianId;

    @Enumerated(EnumType.STRING)
    @Column(name = "relationship_type", nullable = false, length = 20)
    private RelationshipType relationshipType;

    @Column(name = "is_primary_contact")
    @Builder.Default
    private Boolean isPrimaryContact = false;

    @Column(name = "is_emergency_contact")
    @Builder.Default
    private Boolean isEmergencyContact = false;
}
