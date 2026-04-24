package com.servicehomes.api.wishlists.web;

import com.servicehomes.api.identity.application.CurrentUserService;
import com.servicehomes.api.wishlists.application.WishlistService;
import com.servicehomes.api.wishlists.application.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/wishlists")
@RequiredArgsConstructor
public class WishlistController {

    private final CurrentUserService currentUserService;
    private final WishlistService wishlistService;
    @PostMapping
    public ResponseEntity<WishlistSummaryDto> createWishlist(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateWishlistRequest request) {
        return ResponseEntity.ok(wishlistService.createWishlist(currentUserService.requireUserId(jwt), request));
    }

    @GetMapping
    public ResponseEntity<List<WishlistSummaryDto>> listWishlists(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(wishlistService.getWishlistsForUser(currentUserService.requireUserId(jwt)));
    }

    @GetMapping("/contains")
    public ResponseEntity<SavedWishlistIdsDto> getWishlistIdsContainingListing(@AuthenticationPrincipal Jwt jwt, @RequestParam UUID listingId) {
        return ResponseEntity.ok(wishlistService.getWishlistIdsContainingListing(currentUserService.requireUserId(jwt), listingId));
    }

    @GetMapping("/{wishlistId}")
    public ResponseEntity<WishlistDetailDto> getWishlist(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID wishlistId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(wishlistService.getWishlistDetail(currentUserService.requireUserId(jwt), wishlistId, page, size));
    }

    @DeleteMapping("/{wishlistId}")
    public ResponseEntity<Void> deleteWishlist(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID wishlistId) {
        wishlistService.deleteWishlist(currentUserService.requireUserId(jwt), wishlistId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{wishlistId}/items")
    public ResponseEntity<WishlistItemDto> addItem(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID wishlistId, @Valid @RequestBody AddWishlistItemRequest request) {
        return ResponseEntity.ok(wishlistService.addItem(currentUserService.requireUserId(jwt), wishlistId, request));
    }

    @PatchMapping("/{wishlistId}/items/{itemId}")
    public ResponseEntity<WishlistItemDto> updateItem(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID wishlistId, @PathVariable UUID itemId, @Valid @RequestBody UpdateWishlistItemRequest request) {
        return ResponseEntity.ok(wishlistService.updateItem(currentUserService.requireUserId(jwt), wishlistId, itemId, request));
    }

    @DeleteMapping("/{wishlistId}/items/{itemId}")
    public ResponseEntity<Void> removeItem(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID wishlistId, @PathVariable UUID itemId) {
        wishlistService.removeItem(currentUserService.requireUserId(jwt), wishlistId, itemId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{wishlistId}/items/by-listing/{listingId}")
    public ResponseEntity<Void> removeListing(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID wishlistId, @PathVariable UUID listingId) {
        wishlistService.removeListing(currentUserService.requireUserId(jwt), wishlistId, listingId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{wishlistId}/items/reorder")
    public ResponseEntity<Void> reorderItems(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID wishlistId, @Valid @RequestBody ReorderWishlistItemsRequest request) {
        wishlistService.reorderItems(currentUserService.requireUserId(jwt), wishlistId, request);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{wishlistId}/collaborators")
    public ResponseEntity<WishlistDetailDto> updateCollaborators(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID wishlistId, @RequestBody UpdateCollaboratorsRequest request) {
        return ResponseEntity.ok(wishlistService.updateCollaborators(currentUserService.requireUserId(jwt), wishlistId, request));
    }

    @PostMapping("/{wishlistId}/share-link")
    public ResponseEntity<ShareLinkDto> generateShareLink(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID wishlistId) {
        return ResponseEntity.ok(wishlistService.generateShareLink(currentUserService.requireUserId(jwt), wishlistId));
    }

    @PutMapping("/{wishlistId}/privacy")
    public ResponseEntity<WishlistDetailDto> updatePrivacy(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID wishlistId, @RequestParam boolean isPublic) {
        return ResponseEntity.ok(wishlistService.updatePrivacy(currentUserService.requireUserId(jwt), wishlistId, isPublic));
    }

    @PostMapping("/{wishlistId}/cover-upload")
    public ResponseEntity<WishlistCoverUploadResponse> createCoverUpload(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID wishlistId, @Valid @RequestBody WishlistCoverUploadRequest request) {
        return ResponseEntity.ok(wishlistService.createCoverUpload(currentUserService.requireUserId(jwt), wishlistId, request));
    }

    @PutMapping("/{wishlistId}/cover-photo")
    public ResponseEntity<WishlistDetailDto> updateCoverPhoto(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID wishlistId, @Valid @RequestBody FinalizeWishlistCoverRequest request) {
        return ResponseEntity.ok(wishlistService.updateCoverPhoto(currentUserService.requireUserId(jwt), wishlistId, request));
    }

}
