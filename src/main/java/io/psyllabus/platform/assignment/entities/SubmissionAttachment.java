package io.psyllabus.platform.assignment.entities;

import io.psyllabus.platform.common.audit.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "submission_attachments", indexes = {
        @Index(name = "idx_submission_attachment_submission", columnList = "submission_id,tenant_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubmissionAttachment extends Auditable {

    @Id
    @Column(name = "id", nullable = false, length = 255)
    private String id;

    @Column(name = "submission_id", nullable = false)
    private UUID submissionId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "url", nullable = false)
    private String url;

    @Column(name = "path", nullable = false)
    private String path;

    @Column(name = "mime_type", nullable = false, length = 150)
    private String mimeType;

    @Column(name = "size", nullable = false)
    private Long size;

    @Column(name = "original_filename", length = 255)
    private String originalFilename;
}
