package com.servicehomes.api.listings.web;

import com.servicehomes.api.identity.application.CurrentUserService;
import com.servicehomes.api.listings.application.AvailabilityService;
import com.servicehomes.api.listings.application.dto.ListingAvailabilityResponse;
import com.servicehomes.api.listings.application.dto.ListingCalendarResponse;
import com.servicehomes.api.listings.application.dto.UpdateListingAvailabilityRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/listings/{listingId}")
@RequiredArgsConstructor
public class ListingAvailabilityController {

    private final CurrentUserService currentUserService;
    private final AvailabilityService availabilityService;

    @GetMapping("/availability")
    public ResponseEntity<ListingAvailabilityResponse> getAvailability(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable UUID listingId
    ) {
        UUID hostId = currentUserService.requireUserId(jwt);
        return ResponseEntity.ok(availabilityService.getAvailability(hostId, listingId));
    }

    @PutMapping("/availability")
    public ResponseEntity<ListingAvailabilityResponse> updateAvailability(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable UUID listingId,
        @Valid @RequestBody UpdateListingAvailabilityRequest request
    ) {
        UUID hostId = currentUserService.requireUserId(jwt);
        return ResponseEntity.ok(availabilityService.updateAvailability(hostId, listingId, request));
    }

    @GetMapping("/calendar")
    public ResponseEntity<ListingCalendarResponse> getCalendar(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable UUID listingId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        UUID hostId = currentUserService.requireUserId(jwt);
        return ResponseEntity.ok(availabilityService.getCalendar(hostId, listingId, startDate, endDate));
    }
}
