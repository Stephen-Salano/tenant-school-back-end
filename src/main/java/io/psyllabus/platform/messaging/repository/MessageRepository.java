package io.psyllabus.platform.messaging.repository;

import io.psyllabus.platform.messaging.entities.Message;
import io.psyllabus.platform.messaging.enums.MessageAudience;
import io.psyllabus.platform.messaging.enums.MessageType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID> {

    Optional<Message> findByIdAndTenantId(UUID id, UUID tenantId);

    Page<Message> findAllByTenantIdAndTypeOrderByCreatedAtDesc(UUID tenantId, MessageType type, Pageable pageable);

    /**
     * Fetch notices for a specific audience within a school.
     * Includes "ALL" and "PUBLIC" defaults.
     */
    @Query("""
        SELECT m FROM Message m 
        WHERE m.tenantId = :tenantId 
        AND m.isActive = true 
        AND m.type IN ('NOTICE', 'ANNOUNCEMENT')
        AND (m.targetAudience = :audience OR m.targetAudience = 'ALL' OR m.targetAudience = 'PUBLIC')
    """)
    Page<Message> findBoardNotices(
            @Param("tenantId") UUID tenantId, 
            @Param("audience") MessageAudience audience, 
            Pageable pageable
    );
}
