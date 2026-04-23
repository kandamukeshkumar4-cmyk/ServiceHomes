package com.servicehomes.api.reviews.application;

import com.servicehomes.api.analytics.application.EventPublisher;
import com.servicehomes.api.identity.domain.Profile;
import com.servicehomes.api.identity.domain.User;
import com.servicehomes.api.identity.domain.UserRepository;
import com.servicehomes.api.listings.domain.ListingRepository;
import com.servicehomes.api.reservations.domain.Reservation;
import com.servicehomes.api.reservations.domain.ReservationRepository;
import com.servicehomes.api.reviews.application.dto.AddHostResponseRequest;
import com.servicehomes.api.reviews.application.dto.CreateReviewRequest;
import com.servicehomes.api.reviews.application.dto.ListingReviewsResponse;
import com.servicehomes.api.reviews.application.dto.ReviewDto;
import com.servicehomes.api.reviews.domain.Review;
import com.servicehomes.api.reviews.domain.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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

    private final ReviewRepository reviewRepository;
    private final ReservationRepository reservationRepository;
    private final ListingRepository listingRepository;
    private final UserRepository userRepository;
    private final EventPublisher eventPublisher;

    @Transactional
    public ReviewDto createReview(UUID guestId, UUID reservationId, CreateReviewRequest request) {
        Reservation reservation = reservationRepository.findById(reservationId)
            .orElseThrow(() -> new IllegalArgumentException("Reservation not found"));

        if (!reservation.getGuestId().equals(guestId)) {
            throw new IllegalArgumentException("Not authorized");
        }

        if (reservation.getStatus() != Reservation.Status.CONFIRMED
            && reservation.getStatus() != Reservation.Status.COMPLETED) {
            throw new IllegalArgumentException("Only confirmed or completed reservations can be reviewed");
        }

        if (!reservation.getCheckOut().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Reviews are available only after checkout");
        }

        if (reviewRepository.existsByReservation_Id(reservationId)) {
            throw new IllegalStateException("Reservation has already been reviewed");
        }

        String normalizedComment = request.comment().trim();
        Review review = reviewRepository.save(Review.builder()
            .reservation(reservation)
            .listing(reservation.getListing())
            .guestId(reservation.getGuestId())
            .rating(request.rating())
            .comment(normalizedComment)
            .build());

        eventPublisher.publish(
            "review_created",
            "review",
            review.getId().toString(),
            Map.of(
                "reviewId", review.getId().toString(),
                "reservationId", reservation.getId().toString(),
                "listingId", reservation.getListing().getId().toString(),
                "guestId", reservation.getGuestId().toString(),
                "rating", request.rating(),
                "commentLength", normalizedComment.length()
            )
        );

        return toDto(review, userRepository.findById(review.getGuestId()).orElse(null));
    }

    @Transactional
    public ReviewDto addHostResponse(UUID hostId, UUID reviewId, AddHostResponseRequest request) {
        Review review = reviewRepository.findById(reviewId)
            .orElseThrow(() -> new IllegalArgumentException("Review not found"));

        if (!review.getListing().getHostId().equals(hostId)) {
            throw new IllegalArgumentException("Not authorized");
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
        if (!listingRepository.existsById(listingId)) {
            throw new IllegalArgumentException("Listing not found");
        }

        Page<Review> page = reviewRepository.findByListing_IdOrderByCreatedAtDesc(listingId, pageable);
        List<UUID> guestIds = page.getContent().stream()
            .map(Review::getGuestId)
            .distinct()
            .toList();

        Map<UUID, User> usersById = StreamSupport.stream(userRepository.findAllById(guestIds).spliterator(), false)
            .collect(Collectors.toMap(User::getId, Function.identity()));

        double averageRating = reviewRepository.calculateAverageRatingByListingId(listingId) != null
            ? reviewRepository.calculateAverageRatingByListingId(listingId)
            : 0.0d;

        List<ReviewDto> content = page.getContent().stream()
            .map(review -> toDto(review, usersById.get(review.getGuestId())))
            .toList();

        return new ListingReviewsResponse(
            averageRating,
            reviewRepository.countReviewsByListingId(listingId),
            content,
            page.getTotalElements(),
            page.getTotalPages(),
            page.getNumber(),
            page.getSize(),
            page.isFirst(),
            page.isLast()
        );
    }

    private ReviewDto toDto(Review review, User guest) {
        Profile profile = guest != null ? guest.getProfile() : null;
        return new ReviewDto(
            review.getId(),
            review.getReservation().getId(),
            review.getListing().getId(),
            review.getGuestId(),
            review.getRating(),
            review.getComment(),
            profile != null && profile.getDisplayName() != null ? profile.getDisplayName() : "Guest",
            profile != null ? profile.getAvatarUrl() : null,
            review.getHostResponse(),
            review.getCreatedAt(),
            review.getUpdatedAt()
        );
    }
}
