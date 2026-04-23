package com.servicehomes.api.reviews.web;

import com.servicehomes.api.identity.application.CurrentUserService;
import com.servicehomes.api.reviews.application.ReviewService;
import com.servicehomes.api.reviews.application.dto.AddHostResponseRequest;
import com.servicehomes.api.reviews.application.dto.CreateHostReviewRequest;
import com.servicehomes.api.reviews.application.dto.CreateReviewRequest;
import com.servicehomes.api.reviews.application.dto.ListingReviewsResponse;
import com.servicehomes.api.reviews.application.dto.ModerateReviewRequest;
import com.servicehomes.api.reviews.application.dto.ModerationReviewDto;
import com.servicehomes.api.reviews.application.dto.ReportReviewRequest;
import com.servicehomes.api.reviews.application.dto.ReviewDto;
import com.servicehomes.api.reviews.application.dto.ReviewReportDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
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

    @PostMapping("/reservations/{id}/host-review")
    public ResponseEntity<ReviewDto> createHostReview(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable UUID id,
        @Valid @RequestBody CreateHostReviewRequest request
    ) {
        UUID hostId = currentUserService.requireUserId(jwt);
        return ResponseEntity.ok(reviewService.createHostReview(hostId, id, request));
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

    @PostMapping("/reviews/{id}/report")
    public ResponseEntity<ReviewReportDto> reportReview(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable UUID id,
        @Valid @RequestBody ReportReviewRequest request
    ) {
        UUID reporterId = currentUserService.requireUserId(jwt);
        return ResponseEntity.ok(reviewService.reportReview(reporterId, id, request));
    }

    @GetMapping("/admin/reviews/moderation")
    public ResponseEntity<Page<ModerationReviewDto>> listModerationQueue(
        @AuthenticationPrincipal Jwt jwt,
        @PageableDefault(size = 20) Pageable pageable
    ) {
        UUID adminId = currentUserService.requireUserId(jwt);
        return ResponseEntity.ok(reviewService.listModerationQueue(adminId, pageable));
    }

    @PatchMapping("/admin/reviews/{id}/moderation")
    public ResponseEntity<ReviewDto> moderateReview(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable UUID id,
        @Valid @RequestBody ModerateReviewRequest request
    ) {
        UUID adminId = currentUserService.requireUserId(jwt);
        return ResponseEntity.ok(reviewService.moderateReview(adminId, id, request));
    }

    @GetMapping("/listings/{id}/reviews")
    public ResponseEntity<ListingReviewsResponse> getListingReviews(
        @PathVariable UUID id,
        @PageableDefault(size = 10) Pageable pageable
    ) {
        return ResponseEntity.ok(reviewService.getListingReviews(id, pageable));
    }
}
