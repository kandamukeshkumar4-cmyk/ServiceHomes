package com.servicehomes.api.listings.web;

import com.servicehomes.api.listings.application.ListingService;
import com.servicehomes.api.listings.application.dto.CreateListingRequest;
import com.servicehomes.api.listings.application.dto.ListingDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/listings")
@RequiredArgsConstructor
public class ListingController {

    private final ListingService listingService;

    @PostMapping
    public ResponseEntity<ListingDto> create(
        @AuthenticationPrincipal Jwt jwt,
        @Valid @RequestBody CreateListingRequest request
    ) {
        UUID hostId = UUID.fromString(jwt.getClaimAsString("sub"));
        return ResponseEntity.ok(listingService.create(hostId, request));
    }

    @GetMapping("/my")
    public ResponseEntity<List<ListingDto>> myListings(@AuthenticationPrincipal Jwt jwt) {
        UUID hostId = UUID.fromString(jwt.getClaimAsString("sub"));
        return ResponseEntity.ok(listingService.listByHost(hostId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ListingDto> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(listingService.getById(id));
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
