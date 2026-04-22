package com.servicehomes.api.saved.web;

import com.servicehomes.api.identity.application.CurrentUserService;
import com.servicehomes.api.listings.application.dto.ListingSearchResult;
import com.servicehomes.api.saved.application.SavedListingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/saved-listings")
@RequiredArgsConstructor
public class SavedListingController {

    private final CurrentUserService currentUserService;
    private final SavedListingService savedListingService;

    @GetMapping
    public ResponseEntity<List<ListingSearchResult>> listSavedListings(@AuthenticationPrincipal Jwt jwt) {
        UUID guestId = currentUserService.requireUserId(jwt);
        return ResponseEntity.ok(savedListingService.listSavedListings(guestId));
    }

    @PutMapping("/{listingId}")
    public ResponseEntity<Void> save(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID listingId) {
        UUID guestId = currentUserService.requireUserId(jwt);
        savedListingService.save(guestId, listingId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{listingId}")
    public ResponseEntity<Void> unsave(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID listingId) {
        UUID guestId = currentUserService.requireUserId(jwt);
        savedListingService.unsave(guestId, listingId);
        return ResponseEntity.noContent().build();
    }
}
