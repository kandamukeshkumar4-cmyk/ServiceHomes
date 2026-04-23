package com.servicehomes.api.notifications.api;

import com.servicehomes.api.identity.application.CurrentUserService;
import com.servicehomes.api.notifications.application.NotificationPreferencesService;
import com.servicehomes.api.notifications.application.dto.NotificationPreferenceDto;
import com.servicehomes.api.notifications.application.dto.UpdateNotificationPreferenceRequest;
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
@RequestMapping("/notifications/preferences")
public class NotificationPreferencesController {

    private final CurrentUserService currentUserService;
    private final NotificationPreferencesService preferencesService;

    @GetMapping
    public ResponseEntity<List<NotificationPreferenceDto>> getPreferences(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = currentUserService.requireUserId(jwt);
        return ResponseEntity.ok(preferencesService.getPreferences(userId));
    }

    @PutMapping
    public ResponseEntity<NotificationPreferenceDto> updatePreference(
        @AuthenticationPrincipal Jwt jwt,
        @Valid @RequestBody UpdateNotificationPreferenceRequest request
    ) {
        UUID userId = currentUserService.requireUserId(jwt);
        return ResponseEntity.ok(preferencesService.updatePreference(
            userId,
            request.type(),
            request.channel(),
            request.enabled()
        ));
    }
}
