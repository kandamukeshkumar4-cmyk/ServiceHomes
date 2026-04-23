package com.servicehomes.api.reviews.application;

import com.servicehomes.api.analytics.application.EventPublisher;
import com.servicehomes.api.identity.domain.Profile;
import com.servicehomes.api.identity.domain.Role;
import com.servicehomes.api.identity.domain.User;
import com.servicehomes.api.identity.domain.UserRepository;
import com.servicehomes.api.listings.domain.Listing;
import com.servicehomes.api.listings.domain.ListingRepository;
import com.servicehomes.api.reservations.domain.Reservation;
import com.servicehomes.api.reservations.domain.ReservationRepository;
import com.servicehomes.api.reviews.application.dto.*;
import com.servicehomes.api.reviews.domain.ListingRatingAggregate;
import com.servicehomes.api.reviews.domain.Review;
import com.servicehomes.api.reviews.domain.ReviewReport;
import com.servicehomes.api.reviews.domain.ReviewReportRepository;
import com.servicehomes.api.reviews.domain.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {

    private static final int DOUBLE_BLIND_DAYS = 14;

    private final ReviewRepository reviewRepository;
    private final ReviewReportRepository reviewReportRepository;
    private final ReservationRepository reservationRepository;
    private final ListingRepository listingRepository;
    private final UserRepository userRepository;
    private final EventPublisher eventPublisher;

    @Transactional
    public ReviewDto createReview(UUID guestId, UUID reservationId, CreateReviewRequest request) {
        Reservation reservation = requireCompletedReservation(reservationId);
        if (!reservation.getGuestId().equals(guestId)) {
            throw new IllegalArgumentException("Not authorized");
        }
        if (reviewRepository.existsByReservation_IdAndReviewerRole(reservationId, Review.ReviewerRole.GUEST)) {
            throw new IllegalStateException("Reservation has already been reviewed by the guest");
        }

        String normalizedComment = request.comment().trim();
        Review review = Review.builder()
            .reservation(reservation)
            .listing(reservation.getListing())
            .guestId(reservation.getGuestId())
            .hostId(reservation.getListing().getHostId())
            .reviewerId(guestId)
            .reviewerRole(Review.ReviewerRole.GUEST)
            .rating(request.rating())
            .cleanlinessRating(request.cleanlinessRating())
            .accuracyRating(request.accuracyRating())
            .communicationRating(request.communicationRating())
            .locationRating(request.locationRating())
            .valueRating(request.valueRating())
            .comment(normalizedComment)
            .visibleAt(resolveVisibleAt(reservation, Review.ReviewerRole.HOST))
            .build();

        Review saved = reviewRepository.save(review);
        revealCounterpartIfPresent(reservationId, Review.ReviewerRole.HOST);
        refreshListingRatingCache(reservation.getListing().getId());

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("reviewId", saved.getId().toString());
        payload.put("reservationId", reservation.getId().toString());
        payload.put("listingId", reservation.getListing().getId().toString());
        payload.put("guestId", reservation.getGuestId().toString());
        payload.put("hostId", reservation.getListing().getHostId().toString());
        payload.put("rating", request.rating());
        payload.put("cleanlinessRating", request.cleanlinessRating());
        payload.put("accuracyRating", request.accuracyRating());
        payload.put("communicationRating", request.communicationRating());
        payload.put("locationRating", request.locationRating());
        payload.put("valueRating", request.valueRating());
        payload.put("commentLength", normalizedComment.length());
        payload.put("visibleAt", saved.getVisibleAt().toString());

        eventPublisher.publish("review_created", "review", saved.getId().toString(), payload);

        return toDto(saved, userRepository.findById(saved.getGuestId()).orElse(null));
    }

    @Transactional
    public ReviewDto createHostReview(UUID hostId, UUID reservationId, CreateHostReviewRequest request) {
        Reservation reservation = requireCompletedReservation(reservationId);
        if (!reservation.getListing().getHostId().equals(hostId)) {
            throw new IllegalArgumentException("Not authorized");
        }
        if (reviewRepository.existsByReservation_IdAndReviewerRole(reservationId, Review.ReviewerRole.HOST)) {
            throw new IllegalStateException("Reservation has already been reviewed by the host");
        }

        String normalizedComment = request.comment().trim();
        Review review = Review.builder()
            .reservation(reservation)
            .listing(reservation.getListing())
            .guestId(reservation.getGuestId())
            .hostId(hostId)
            .reviewerId(hostId)
            .reviewerRole(Review.ReviewerRole.HOST)
            .rating(request.rating())
            .comment(normalizedComment)
            .visibleAt(resolveVisibleAt(reservation, Review.ReviewerRole.GUEST))
            .build();

        Review saved = reviewRepository.save(review);
        revealCounterpartIfPresent(reservationId, Review.ReviewerRole.GUEST);
        refreshListingRatingCache(reservation.getListing().getId());

        eventPublisher.publish(
            "host_review_created",
            "review",
            saved.getId().toString(),
            Map.of(
                "reviewId", saved.getId().toString(),
                "reservationId", reservation.getId().toString(),
                "listingId", reservation.getListing().getId().toString(),
                "hostId", hostId.toString(),
                "guestId", reservation.getGuestId().toString(),
                "rating", request.rating(),
                "commentLength", normalizedComment.length(),
                "visibleAt", saved.getVisibleAt().toString()
            )
        );

        return toDto(saved, userRepository.findById(saved.getGuestId()).orElse(null));
    }

    @Transactional
    public ReviewDto addHostResponse(UUID hostId, UUID reviewId, AddHostResponseRequest request) {
        Review review = reviewRepository.findById(reviewId)
            .orElseThrow(() -> new IllegalArgumentException("Review not found"));

        if (review.getReviewerRole() != Review.ReviewerRole.GUEST) {
            throw new IllegalArgumentException("Hosts can only respond to guest reviews");
        }
        if (!review.getListing().getHostId().equals(hostId)) {
            throw new IllegalArgumentException("Not authorized");
        }
        if (!isPublic(review, Instant.now())) {
            throw new IllegalStateException("Review is not visible yet");
        }
        if (review.getHostResponse() != null && !review.getHostResponse().isBlank()) {
            throw new IllegalStateException("Host response already exists");
        }

        String normalizedResponse = request.response().trim();
        review.setHostResponse(normalizedResponse);

        eventPublisher.publish(
            "host_response_added",
            "review",
            review.getId().toString(),
            Map.of(
                "reviewId", review.getId().toString(),
                "reservationId", review.getReservation().getId().toString(),
                "listingId", review.getListing().getId().toString(),
                "hostId", hostId.toString(),
                "responseLength", normalizedResponse.length()
            )
        );

        return toDto(review, userRepository.findById(review.getGuestId()).orElse(null));
    }

    public ListingReviewsResponse getListingReviews(UUID listingId, Pageable pageable) {
        Listing listing = listingRepository.findById(listingId)
            .orElseThrow(() -> new IllegalArgumentException("Listing not found"));

        Instant now = Instant.now();
        Page<Review> page = reviewRepository
            .findByListing_IdAndReviewerRoleAndModerationStatusAndVisibleAtLessThanEqualOrderByCreatedAtDesc(
                listingId,
                Review.ReviewerRole.GUEST,
                Review.ModerationStatus.APPROVED,
                now,
                pageable
            );
        Map<UUID, User> usersById = loadUsers(page.getContent().stream().map(Review::getGuestId).distinct().toList());
        List<ReviewDto> content = page.getContent().stream()
            .map(review -> toDto(review, usersById.get(review.getGuestId())))
            .toList();

        ListingRatingAggregate aggregate = reviewRepository.calculateListingRatingAggregate(listingId, now)
            .orElse(new ListingRatingAggregate(listingId, null, 0, null, null, null, null, null));

        return new ListingReviewsResponse(
            aggregate.averageRating() != null ? aggregate.averageRating() : 0.0d,
            aggregate.reviewCount(),
            aggregate.cleanlinessRating(),
            aggregate.accuracyRating(),
            aggregate.communicationRating(),
            aggregate.locationRating(),
            aggregate.valueRating(),
            trustScore(aggregate).doubleValue(),
            content,
            page.getTotalElements(),
            page.getTotalPages(),
            page.getNumber(),
            page.getSize(),
            page.isFirst(),
            page.isLast()
        );
    }

    @Transactional
    public ReviewReportDto reportReview(UUID reporterId, UUID reviewId, ReportReviewRequest request) {
        Review review = reviewRepository.findById(reviewId)
            .orElseThrow(() -> new IllegalArgumentException("Review not found"));
        if (!isPublic(review, Instant.now())) {
            throw new IllegalArgumentException("Review not found");
        }
        if (reviewReportRepository.existsByReview_IdAndReporterId(reviewId, reporterId)) {
            throw new IllegalStateException("Review has already been reported by this user");
        }

        ReviewReport report = reviewReportRepository.save(ReviewReport.builder()
            .review(review)
            .reporterId(reporterId)
            .reason(parseReason(request.reason()))
            .details(stripToNull(request.details()))
            .build());
        review.setReportCount(review.getReportCount() + 1);

        eventPublisher.publish(
            "review_reported",
            "review",
            review.getId().toString(),
            Map.of(
                "reviewId", review.getId().toString(),
                "listingId", review.getListing().getId().toString(),
                "reporterId", reporterId.toString(),
                "reason", report.getReason().name(),
                "reportCount", review.getReportCount()
            )
        );

        return toReportDto(report);
    }

    public Page<ModerationReviewDto> listModerationQueue(UUID adminId, Pageable pageable) {
        requireAdmin(adminId);
        return reviewReportRepository.findByStatusOrderByCreatedAtDesc(ReviewReport.Status.OPEN, pageable)
            .map(report -> new ModerationReviewDto(
                toDto(report.getReview(), userRepository.findById(report.getReview().getGuestId()).orElse(null)),
                List.of(toReportDto(report))
            ));
    }

    @Transactional
    public ReviewDto moderateReview(UUID adminId, UUID reviewId, ModerateReviewRequest request) {
        requireAdmin(adminId);
        Review review = reviewRepository.findById(reviewId)
            .orElseThrow(() -> new IllegalArgumentException("Review not found"));
        Review.ModerationStatus moderationStatus = parseModerationStatus(request.moderationStatus());
        review.setModerationStatus(moderationStatus);
        review.setModeratedAt(Instant.now());
        review.setModeratedBy(adminId);
        review.setModerationNotes(stripToNull(request.notes()));

        ReviewReport.Status reportStatus = moderationStatus == Review.ModerationStatus.HIDDEN
            ? ReviewReport.Status.RESOLVED
            : ReviewReport.Status.DISMISSED;
        reviewReportRepository.findByReview_IdAndStatus(reviewId, ReviewReport.Status.OPEN)
            .forEach(report -> {
                report.setStatus(reportStatus);
                report.setResolvedAt(Instant.now());
                report.setResolvedBy(adminId);
            });

        refreshListingRatingCache(review.getListing().getId());
        eventPublisher.publish(
            "review_moderated",
            "review",
            review.getId().toString(),
            Map.of(
                "reviewId", review.getId().toString(),
                "listingId", review.getListing().getId().toString(),
                "moderatorId", adminId.toString(),
                "moderationStatus", moderationStatus.name()
            )
        );

        return toDto(review, userRepository.findById(review.getGuestId()).orElse(null));
    }

    @Scheduled(initialDelay = 3600000, fixedDelay = 3600000)
    @Transactional
    public void refreshVisibleReviewCaches() {
        reviewRepository.findListingIdsWithVisibleGuestReviews(Instant.now())
            .forEach(this::refreshListingRatingCache);
    }

    private Reservation requireCompletedReservation(UUID reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
            .orElseThrow(() -> new IllegalArgumentException("Reservation not found"));
        if (reservation.getStatus() != Reservation.Status.COMPLETED) {
            throw new IllegalArgumentException("Only completed reservations can be reviewed");
        }
        if (!reservation.getCheckOut().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Reviews are available only after checkout");
        }
        return reservation;
    }

    private Instant resolveVisibleAt(Reservation reservation, Review.ReviewerRole counterpartRole) {
        Instant now = Instant.now();
        if (reviewRepository.findByReservation_IdAndReviewerRole(reservation.getId(), counterpartRole).isPresent()) {
            return now;
        }

        Instant timeout = reservation.getCheckOut()
            .plusDays(DOUBLE_BLIND_DAYS)
            .atStartOfDay()
            .toInstant(ZoneOffset.UTC);
        return now.isBefore(timeout) ? timeout : now;
    }

    private void revealCounterpartIfPresent(UUID reservationId, Review.ReviewerRole counterpartRole) {
        reviewRepository.findByReservation_IdAndReviewerRole(reservationId, counterpartRole)
            .ifPresent(counterpart -> {
                if (counterpart.getVisibleAt().isAfter(Instant.now())) {
                    counterpart.setVisibleAt(Instant.now());
                }
            });
    }

    private boolean isPublic(Review review, Instant now) {
        return review.getReviewerRole() == Review.ReviewerRole.GUEST
            && review.getModerationStatus() == Review.ModerationStatus.APPROVED
            && !review.getVisibleAt().isAfter(now);
    }

    private void refreshListingRatingCache(UUID listingId) {
        Listing listing = listingRepository.findById(listingId)
            .orElseThrow(() -> new IllegalArgumentException("Listing not found"));
        ListingRatingAggregate aggregate = reviewRepository.calculateListingRatingAggregate(listingId, Instant.now())
            .orElse(new ListingRatingAggregate(listingId, null, 0, null, null, null, null, null));

        listing.setAverageRating(toRating(aggregate.averageRating()));
        listing.setReviewCount(aggregate.reviewCount());
        listing.setCleanlinessRating(toRating(aggregate.cleanlinessRating()));
        listing.setAccuracyRating(toRating(aggregate.accuracyRating()));
        listing.setCommunicationRating(toRating(aggregate.communicationRating()));
        listing.setLocationRating(toRating(aggregate.locationRating()));
        listing.setValueRating(toRating(aggregate.valueRating()));
        listing.setTrustScore(trustScore(aggregate));
        listingRepository.save(listing);
    }

    private BigDecimal trustScore(ListingRatingAggregate aggregate) {
        if (aggregate.averageRating() == null || aggregate.reviewCount() == 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        double score = Math.min(100.0d, aggregate.averageRating() * 16.0d + Math.min(aggregate.reviewCount(), 20));
        return BigDecimal.valueOf(score).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal toRating(Double value) {
        return value == null ? null : BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }

    private Map<UUID, User> loadUsers(List<UUID> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return StreamSupport.stream(userRepository.findAllById(userIds).spliterator(), false)
            .collect(Collectors.toMap(User::getId, Function.identity()));
    }

    private void requireAdmin(UUID userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        boolean isAdmin = user.getRoles().stream()
            .anyMatch(role -> role.getName() == Role.RoleName.ADMIN);
        if (!isAdmin) {
            throw new IllegalArgumentException("Admin access required");
        }
    }

    private ReviewReport.Reason parseReason(String rawReason) {
        try {
            return ReviewReport.Reason.valueOf(rawReason.trim().toUpperCase().replace('-', '_').replace(' ', '_'));
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("Unsupported review report reason");
        }
    }

    private Review.ModerationStatus parseModerationStatus(String rawStatus) {
        try {
            return Review.ModerationStatus.valueOf(rawStatus.trim().toUpperCase());
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("Unsupported moderation status");
        }
    }

    private ReviewDto toDto(Review review, User guest) {
        Profile profile = guest != null ? guest.getProfile() : null;
        return new ReviewDto(
            review.getId(),
            review.getReservation().getId(),
            review.getListing().getId(),
            review.getGuestId(),
            review.getHostId(),
            review.getReviewerId(),
            review.getReviewerRole().name(),
            review.getRating(),
            review.getCleanlinessRating(),
            review.getAccuracyRating(),
            review.getCommunicationRating(),
            review.getLocationRating(),
            review.getValueRating(),
            review.getComment(),
            profile != null && profile.getDisplayName() != null ? profile.getDisplayName() : "Guest",
            profile != null ? profile.getAvatarUrl() : null,
            review.getHostResponse(),
            review.getVisibleAt(),
            review.getModerationStatus().name(),
            review.getReportCount(),
            review.getCreatedAt(),
            review.getUpdatedAt()
        );
    }

    private ReviewReportDto toReportDto(ReviewReport report) {
        return new ReviewReportDto(
            report.getId(),
            report.getReview().getId(),
            report.getReporterId(),
            report.getReason().name(),
            report.getDetails(),
            report.getStatus().name(),
            report.getCreatedAt(),
            report.getResolvedAt(),
            report.getResolvedBy()
        );
    }

    private String stripToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
