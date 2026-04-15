package io.psyllabus.platform.messaging.service.impl;

import io.psyllabus.platform.audit.enums.AuditAction;
import io.psyllabus.platform.audit.service.AuditService;
import io.psyllabus.platform.common.exception.ResourceNotFoundException;
import io.psyllabus.platform.messaging.dto.request.MessageBoardRequest;
import io.psyllabus.platform.messaging.dto.response.MessageResponse;
import io.psyllabus.platform.messaging.entities.Message;
import io.psyllabus.platform.messaging.enums.MessageAudience;
import io.psyllabus.platform.messaging.enums.MessageType;
import io.psyllabus.platform.messaging.repository.MessageRepository;
import io.psyllabus.platform.messaging.service.MessageService;
import io.psyllabus.platform.messaging.service.mapper.MessageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class MessageServiceImpl implements MessageService {

    private static final String MESSAGE_NOT_FOUND = "Message not found: ";
    public static final String MESSAGE = "MESSAGE";
    private final MessageRepository messageRepository;
    private final MessageMapper messageMapper;
    private final AuditService auditService;

    @Override
    public MessageResponse postNotice(UUID tenantId, MessageBoardRequest request) {
        Message message = Message.builder()
                .tenantId(tenantId)
                .type(request.type())
                .targetAudience(request.targetAudience())
                .title(request.title())
                .content(request.content())
                .attachmentUrl(request.attachmentUrl())
                .isActive(true)
                .build();
        
        message = messageRepository.save(message);
        log.info("Notice posted: id={}, tenant={}, audience={}", message.getId(), tenantId, message.getTargetAudience());
        
        auditService.log(AuditAction.CREATE, MESSAGE, message.getId(), "Posted new notice: " + message.getTitle());
        
        return messageMapper.toResponse(message);
    }

    @Override
    @Transactional(readOnly = true)
    public MessageResponse getMessage(UUID id, UUID tenantId) {
        return messageRepository.findByIdAndTenantId(id, tenantId)
                .map(messageMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException(MESSAGE_NOT_FOUND + id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MessageResponse> getBoard(UUID tenantId, MessageAudience audience, Pageable pageable) {
        return messageRepository.findBoardNotices(tenantId, audience, pageable)
                .map(messageMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MessageResponse> getAllMessages(UUID tenantId, Pageable pageable) {
        return messageRepository.findAllByTenantIdAndTypeOrderByCreatedAtDesc(tenantId, MessageType.NOTICE, pageable)
                .map(messageMapper::toResponse);
    }

    @Override
    public MessageResponse updateNotice(UUID id, UUID tenantId, MessageBoardRequest request) {
        Message message = messageRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(MESSAGE_NOT_FOUND + id));
        
        message.setTitle(request.title());
        message.setContent(request.content());
        message.setTargetAudience(request.targetAudience());
        message.setType(request.type());
        message.setAttachmentUrl(request.attachmentUrl());
        
        Message saved = messageRepository.save(message);
        log.info("Notice updated: id={}, tenant={}", id, tenantId);

        auditService.log(AuditAction.UPDATE, MESSAGE, id, "Updated notice: " + saved.getTitle());

        return messageMapper.toResponse(saved);
    }

    @Override
    public void deleteMessage(UUID id, UUID tenantId) {
        Message message = messageRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(MESSAGE_NOT_FOUND + id));
        
        message.setActive(false); // Soft delete
        messageRepository.save(message);
        log.info("Message deleted (soft): id={}, tenant={}", id, tenantId);

        auditService.log(AuditAction.DELETE, MESSAGE, id, "Deleted message: " + message.getTitle());
    }
}
