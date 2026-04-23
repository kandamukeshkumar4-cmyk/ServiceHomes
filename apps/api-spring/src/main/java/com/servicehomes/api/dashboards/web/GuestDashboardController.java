package com.servicehomes.api.dashboards.web;

import com.servicehomes.api.analytics.application.EventPublisher;
import com.servicehomes.api.dashboards.application.GuestDashboardService;
import com.servicehomes.api.dashboards.application.dto.GuestDashboardResponse;
import com.servicehomes.api.identity.application.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/guest/dashboard")
@RequiredArgsConstructor
public class GuestDashboardController {

    private final GuestDashboardService guestDashboardService;
    private final CurrentUserService currentUserService;
    private final EventPublisher eventPublisher;

    @GetMapping
    public ResponseEntity<GuestDashboardResponse> getDashboard(@AuthenticationPrincipal Jwt jwt) {
        UUID guestId = currentUserService.requireUserId(jwt);
        GuestDashboardResponse response = guestDashboardService.getDashboard(guestId);
        eventPublisher.publish("guest_dashboard_viewed", "dashboard", guestId.toString(),
            Map.of("guestId", guestId.toString(), "upcomingTrips", response.upcomingTrips().size()));
        return ResponseEntity.ok(response);
    }
}
