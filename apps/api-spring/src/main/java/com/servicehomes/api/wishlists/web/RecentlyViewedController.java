package com.servicehomes.api.wishlists.web;

import com.servicehomes.api.identity.application.CurrentUserService;
import com.servicehomes.api.wishlists.application.RecentlyViewedService;
import com.servicehomes.api.wishlists.application.dto.ListingSummaryDto;
import com.servicehomes.api.wishlists.application.dto.RecordListingViewRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/recently-viewed")
@RequiredArgsConstructor
public class RecentlyViewedController {

    private final CurrentUserService currentUserService;
    private final RecentlyViewedService recentlyViewedService;

    @PostMapping
    public ResponseEntity<Void> recordView(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody RecordListingViewRequest request) {
        recentlyViewedService.recordView(currentUserService.requireUserId(jwt), request.listingId(), request.sourcePage());
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<ListingSummaryDto>> getRecentlyViewed(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(recentlyViewedService.getRecentlyViewed(currentUserService.requireUserId(jwt)));
    }

    @DeleteMapping
    public ResponseEntity<Void> clearHistory(@AuthenticationPrincipal Jwt jwt) {
        recentlyViewedService.clearHistory(currentUserService.requireUserId(jwt));
        return ResponseEntity.noContent().build();
    }
}
