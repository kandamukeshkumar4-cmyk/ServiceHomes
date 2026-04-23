package com.servicehomes.api.dashboards.web;

import com.servicehomes.api.analytics.application.EventPublisher;
import com.servicehomes.api.dashboards.application.HostDashboardService;
import com.servicehomes.api.dashboards.application.dto.HostDashboardResponse;
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
@RequestMapping("/host/dashboard")
@RequiredArgsConstructor
public class HostDashboardController {

    private final HostDashboardService hostDashboardService;
    private final CurrentUserService currentUserService;
    private final EventPublisher eventPublisher;

    @GetMapping
    public ResponseEntity<HostDashboardResponse> getDashboard(@AuthenticationPrincipal Jwt jwt) {
        UUID hostId = currentUserService.requireUserId(jwt);
        HostDashboardResponse response = hostDashboardService.getDashboard(hostId);
        eventPublisher.publish("host_dashboard_viewed", "dashboard", hostId.toString(),
            Map.of("hostId", hostId.toString(), "listingCount", response.listingPerformance().size()));
        return ResponseEntity.ok(response);
    }
}
