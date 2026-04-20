package io.psyllabus.platform.onboarding.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "import_batch_outcomes", indexes = {
        @Index(name = "idx_batch_outcome_job_id", columnList = "job_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportBatchOutcome {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "job_id", nullable = false)
    private UUID jobId;

    private int rowNumber;
    private String status; // SUCCESS or FAILED
    private String identifier;
    private String message;
    private String rawValue;
}
