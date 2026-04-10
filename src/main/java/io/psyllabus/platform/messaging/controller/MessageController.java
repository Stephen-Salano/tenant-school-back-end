package io.psyllabus.platform.messaging.controller;

import io.psyllabus.platform.common.controller.BaseController;
import io.psyllabus.platform.common.dto.ApiResponse;
import io.psyllabus.platform.common.dto.PageResponse;
import io.psyllabus.platform.messaging.dto.request.MessageBoardRequest;
import io.psyllabus.platform.messaging.dto.response.MessageResponse;
import io.psyllabus.platform.messaging.enums.MessageAudience;
import io.psyllabus.platform.messaging.service.MessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
@Tag(name = "Messaging", description = "Notice board and communication management")
public class MessageController extends BaseController {

    private final MessageService messageService;

    @PostMapping("/notices")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Post a new notice")
    public ResponseEntity<ApiResponse<MessageResponse>> postNotice(@Valid @RequestBody MessageBoardRequest request) {
        return created(messageService.postNotice(getCurrentTenantId(), request), "Notice posted");
    }

    @GetMapping("/board")
    @Operation(summary = "Fetch notice board for current user's audience")
    public ResponseEntity<ApiResponse<PageResponse<MessageResponse>>> getBoard(
            @RequestParam MessageAudience audience,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        return ok(PageResponse.from(messageService.getBoard(getCurrentTenantId(), audience, pageable)), "Board retrieved");
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get single message detail")
    public ResponseEntity<ApiResponse<MessageResponse>> getMessage(@PathVariable UUID id) {
        return ok(messageService.getMessage(id, getCurrentTenantId()), "Message retrieved");
    }

    @PatchMapping("/notices/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Update a notice")
    public ResponseEntity<ApiResponse<MessageResponse>> updateNotice(
            @PathVariable UUID id,
            @Valid @RequestBody MessageBoardRequest request
    ) {
        return ok(messageService.updateNotice(id, getCurrentTenantId(), request), "Notice updated");
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Delete a message (soft)")
    public ResponseEntity<ApiResponse<Void>> deleteMessage(@PathVariable UUID id) {
        messageService.deleteMessage(id, getCurrentTenantId());
        return ok(null, "Message deleted");
    }
}
