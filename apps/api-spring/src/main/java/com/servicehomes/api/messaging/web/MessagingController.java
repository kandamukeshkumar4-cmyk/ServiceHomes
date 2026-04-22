package com.servicehomes.api.messaging.web;

import com.servicehomes.api.identity.application.CurrentUserService;
import com.servicehomes.api.messaging.application.MessagingService;
import com.servicehomes.api.messaging.application.dto.InboxThreadDto;
import com.servicehomes.api.messaging.application.dto.MessageThreadDto;
import com.servicehomes.api.messaging.application.dto.SendMessageRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class MessagingController {

    private final CurrentUserService currentUserService;
    private final MessagingService messagingService;

    @GetMapping("/inbox")
    public ResponseEntity<List<InboxThreadDto>> inbox(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = currentUserService.requireUserId(jwt);
        return ResponseEntity.ok(messagingService.getInbox(userId));
    }

    @GetMapping("/reservations/{reservationId}/messages")
    public ResponseEntity<MessageThreadDto> getThread(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable UUID reservationId
    ) {
        UUID userId = currentUserService.requireUserId(jwt);
        return ResponseEntity.ok(messagingService.getThread(reservationId, userId));
    }

    @PostMapping("/reservations/{reservationId}/messages")
    public ResponseEntity<MessageThreadDto> sendMessage(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable UUID reservationId,
        @Valid @RequestBody SendMessageRequest request
    ) {
        UUID userId = currentUserService.requireUserId(jwt);
        return ResponseEntity.ok(messagingService.sendMessage(userId, reservationId, request));
    }
}
