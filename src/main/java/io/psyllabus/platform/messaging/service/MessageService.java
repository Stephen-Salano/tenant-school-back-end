package io.psyllabus.platform.messaging.service;

import io.psyllabus.platform.messaging.dto.request.MessageBoardRequest;
import io.psyllabus.platform.messaging.dto.response.MessageResponse;
import io.psyllabus.platform.messaging.enums.MessageAudience;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface MessageService {

    /** Post a new notice/announcement to the board. */
    MessageResponse postNotice(UUID tenantId, MessageBoardRequest request);

    /** Get a single message by ID. */
    MessageResponse getMessage(UUID id, UUID tenantId);

    /** List notices for a specific audience (used by portals). */
    Page<MessageResponse> getBoard(UUID tenantId, MessageAudience audience, Pageable pageable);

    /** Full list of messages for admin management. */
    Page<MessageResponse> getAllMessages(UUID tenantId, Pageable pageable);

    /** Update an existing notice. */
    MessageResponse updateNotice(UUID id, UUID tenantId, MessageBoardRequest request);

    /** Remove a notice (soft delete). */
    void deleteMessage(UUID id, UUID tenantId);
}
