package com.servicehomes.api.reviews.web;

import com.servicehomes.api.identity.application.CurrentUserService;
import com.servicehomes.api.reviews.application.ReviewService;
import com.servicehomes.api.reviews.application.dto.AddHostResponseRequest;
import com.servicehomes.api.reviews.application.dto.CreateReviewRequest;
import com.servicehomes.api.reviews.application.dto.ListingReviewsResponse;
import com.servicehomes.api.reviews.application.dto.ReviewDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ReviewController {

    private final CurrentUserService currentUserService;
    private final ReviewService reviewService;

    @PostMapping("/reservations/{id}/review")
    public ResponseEntity<ReviewDto> createReview(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable UUID id,
        @Valid @RequestBody CreateReviewRequest request
    ) {
        UUID guestId = currentUserService.requireUserId(jwt);
        return ResponseEntity.ok(reviewService.createReview(guestId, id, request));
    }

    @PostMapping("/reviews/{id}/response")
    public ResponseEntity<ReviewDto> addHostResponse(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable UUID id,
        @Valid @RequestBody AddHostResponseRequest request
    ) {
        UUID hostId = currentUserService.requireUserId(jwt);
        return ResponseEntity.ok(reviewService.addHostResponse(hostId, id, request));
    }

    @GetMapping("/listings/{id}/reviews")
    public ResponseEntity<ListingReviewsResponse> getListingReviews(
        @PathVariable UUID id,
        @PageableDefault(size = 10) Pageable pageable
    ) {
        return ResponseEntity.ok(reviewService.getListingReviews(id, pageable));
    }
}
