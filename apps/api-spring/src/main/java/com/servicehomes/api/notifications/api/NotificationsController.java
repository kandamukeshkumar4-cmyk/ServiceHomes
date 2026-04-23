package com.servicehomes.api.notifications.api;

import com.servicehomes.api.identity.application.CurrentUserService;
import com.servicehomes.api.notifications.application.NotificationsService;
import com.servicehomes.api.notifications.application.dto.NotificationDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/notifications")
public class NotificationsController {

    private final CurrentUserService currentUserService;
    private final NotificationsService notificationsService;

    @GetMapping
    public ResponseEntity<Page<NotificationDto>> list(
        @AuthenticationPrincipal Jwt jwt,
        @RequestParam(defaultValue = "false") boolean unreadOnly,
        Pageable pageable
    ) {
        UUID userId = currentUserService.requireUserId(jwt);
        Page<NotificationDto> page = unreadOnly
            ? notificationsService.listUnread(userId, pageable)
            : notificationsService.listAll(userId, pageable);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> unreadCount(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = currentUserService.requireUserId(jwt);
        return ResponseEntity.ok(Map.of("count", notificationsService.getUnreadCount(userId)));
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<NotificationDto> markRead(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        UUID userId = currentUserService.requireUserId(jwt);
        return ResponseEntity.ok(notificationsService.markAsRead(userId, id));
    }

    @PostMapping("/read-all")
    public ResponseEntity<Map<String, Integer>> markAllRead(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = currentUserService.requireUserId(jwt);
        return ResponseEntity.ok(Map.of("updated", notificationsService.markAllAsRead(userId)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> dismiss(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        UUID userId = currentUserService.requireUserId(jwt);
        notificationsService.dismiss(userId, id);
        return ResponseEntity.noContent().build();
    }
}
