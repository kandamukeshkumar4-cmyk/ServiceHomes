package com.servicehomes.api.reviews.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID> {

    boolean existsByReservation_Id(UUID reservationId);

    boolean existsByReservation_IdAndReviewerRole(UUID reservationId, Review.ReviewerRole reviewerRole);

    Optional<Review> findByReservation_IdAndReviewerRole(UUID reservationId, Review.ReviewerRole reviewerRole);

    Page<Review> findByListing_IdAndReviewerRoleAndModerationStatusAndVisibleAtLessThanEqualOrderByCreatedAtDesc(
        UUID listingId,
        Review.ReviewerRole reviewerRole,
        Review.ModerationStatus moderationStatus,
        Instant visibleAt,
        Pageable pageable
    );

    default Double calculateAverageRatingByListingId(UUID listingId, Instant visibleAt) {
        return calculateAverageRatingByListingId(
            listingId,
            Review.ReviewerRole.GUEST,
            Review.ModerationStatus.APPROVED,
            visibleAt
        );
    }

    @Query("""
        SELECT AVG(r.rating)
        FROM Review r
        WHERE r.listing.id = :listingId
          AND r.reviewerRole = :reviewerRole
          AND r.moderationStatus = :moderationStatus
          AND r.visibleAt <= :visibleAt
        """)
    Double calculateAverageRatingByListingId(
        @Param("listingId") UUID listingId,
        @Param("reviewerRole") Review.ReviewerRole reviewerRole,
        @Param("moderationStatus") Review.ModerationStatus moderationStatus,
        @Param("visibleAt") Instant visibleAt
    );

    default long countReviewsByListingId(UUID listingId, Instant visibleAt) {
        return countReviewsByListingId(
            listingId,
            Review.ReviewerRole.GUEST,
            Review.ModerationStatus.APPROVED,
            visibleAt
        );
    }

    @Query("""
        SELECT COUNT(r)
        FROM Review r
        WHERE r.listing.id = :listingId
          AND r.reviewerRole = :reviewerRole
          AND r.moderationStatus = :moderationStatus
          AND r.visibleAt <= :visibleAt
        """)
    long countReviewsByListingId(
        @Param("listingId") UUID listingId,
        @Param("reviewerRole") Review.ReviewerRole reviewerRole,
        @Param("moderationStatus") Review.ModerationStatus moderationStatus,
        @Param("visibleAt") Instant visibleAt
    );

    default List<ListingReviewSummary> findSummariesByListingIds(Collection<UUID> listingIds, Instant visibleAt) {
        return findSummariesByListingIds(
            listingIds,
            Review.ReviewerRole.GUEST,
            Review.ModerationStatus.APPROVED,
            visibleAt
        );
    }

    @Query("""
        SELECT new com.servicehomes.api.reviews.domain.ListingReviewSummary(
            r.listing.id,
            AVG(r.rating),
            COUNT(r)
        )
        FROM Review r
        WHERE r.listing.id IN :listingIds
          AND r.reviewerRole = :reviewerRole
          AND r.moderationStatus = :moderationStatus
          AND r.visibleAt <= :visibleAt
        GROUP BY r.listing.id
        """)
    List<ListingReviewSummary> findSummariesByListingIds(
        @Param("listingIds") Collection<UUID> listingIds,
        @Param("reviewerRole") Review.ReviewerRole reviewerRole,
        @Param("moderationStatus") Review.ModerationStatus moderationStatus,
        @Param("visibleAt") Instant visibleAt
    );

    default Optional<ListingRatingAggregate> calculateListingRatingAggregate(UUID listingId, Instant visibleAt) {
        return calculateListingRatingAggregate(
            listingId,
            Review.ReviewerRole.GUEST,
            Review.ModerationStatus.APPROVED,
            visibleAt
        );
    }

    @Query("""
        SELECT new com.servicehomes.api.reviews.domain.ListingRatingAggregate(
            r.listing.id,
            AVG(r.rating),
            COUNT(r),
            AVG(r.cleanlinessRating),
            AVG(r.accuracyRating),
            AVG(r.communicationRating),
            AVG(r.locationRating),
            AVG(r.valueRating)
        )
        FROM Review r
        WHERE r.listing.id = :listingId
          AND r.reviewerRole = :reviewerRole
          AND r.moderationStatus = :moderationStatus
          AND r.visibleAt <= :visibleAt
        GROUP BY r.listing.id
        """)
    Optional<ListingRatingAggregate> calculateListingRatingAggregate(
        @Param("listingId") UUID listingId,
        @Param("reviewerRole") Review.ReviewerRole reviewerRole,
        @Param("moderationStatus") Review.ModerationStatus moderationStatus,
        @Param("visibleAt") Instant visibleAt
    );

    default List<UUID> findListingIdsWithVisibleGuestReviews(Instant visibleAt) {
        return findListingIdsWithVisibleReviews(
            Review.ReviewerRole.GUEST,
            Review.ModerationStatus.APPROVED,
            visibleAt
        );
    }

    @Query("""
        SELECT DISTINCT r.listing.id
        FROM Review r
        WHERE r.reviewerRole = :reviewerRole
          AND r.moderationStatus = :moderationStatus
          AND r.visibleAt <= :visibleAt
        """)
    List<UUID> findListingIdsWithVisibleReviews(
        @Param("reviewerRole") Review.ReviewerRole reviewerRole,
        @Param("moderationStatus") Review.ModerationStatus moderationStatus,
        @Param("visibleAt") Instant visibleAt
    );
}
