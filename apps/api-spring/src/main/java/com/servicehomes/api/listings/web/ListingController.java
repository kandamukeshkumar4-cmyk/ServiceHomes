package com.servicehomes.api.listings.web;

import com.servicehomes.api.analytics.application.EventPublisher;
import com.servicehomes.api.listings.application.ListingService;
import com.servicehomes.api.listings.application.dto.CreateListingRequest;
import com.servicehomes.api.listings.application.dto.ListingDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/listings")
@RequiredArgsConstructor
public class ListingController {

    private final ListingService listingService;
    private final EventPublisher eventPublisher;

    @PostMapping
    public ResponseEntity<ListingDto> create(
        @AuthenticationPrincipal Jwt jwt,
        @Valid @RequestBody CreateListingRequest request
    ) {
        UUID hostId = UUID.fromString(jwt.getClaimAsString("sub"));
        return ResponseEntity.ok(listingService.create(hostId, request));
    }

    @GetMapping("/my")
    public ResponseEntity<Page<ListingDto>> myListings(
        @AuthenticationPrincipal Jwt jwt,
        @PageableDefault(size = 20) Pageable pageable
    ) {
        UUID hostId = UUID.fromString(jwt.getClaimAsString("sub"));
        return ResponseEntity.ok(listingService.listByHost(hostId, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ListingDto> getById(@PathVariable UUID id) {
        ListingDto dto = listingService.getById(id);
        eventPublisher.publish("listing_viewed", "listing", id.toString(), Map.of("listingId", id.toString()));
        return ResponseEntity.ok(dto);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ListingDto> update(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable UUID id,
        @Valid @RequestBody CreateListingRequest request
    ) {
        UUID hostId = UUID.fromString(jwt.getClaimAsString("sub"));
        return ResponseEntity.ok(listingService.update(hostId, id, request));
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<ListingDto> publish(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable UUID id
    ) {
        UUID hostId = UUID.fromString(jwt.getClaimAsString("sub"));
        return ResponseEntity.ok(listingService.publish(hostId, id));
    }

    @PostMapping("/{id}/unpublish")
    public ResponseEntity<ListingDto> unpublish(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable UUID id
    ) {
        UUID hostId = UUID.fromString(jwt.getClaimAsString("sub"));
        return ResponseEntity.ok(listingService.unpublish(hostId, id));
    }
}
