package com.servicehomes.api.reviews.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID> {

    boolean existsByReservation_Id(UUID reservationId);

    Page<Review> findByListing_IdOrderByCreatedAtDesc(UUID listingId, Pageable pageable);

    @Query("""
        SELECT AVG(r.rating)
        FROM Review r
        WHERE r.listing.id = :listingId
        """)
    Double calculateAverageRatingByListingId(@Param("listingId") UUID listingId);

    @Query("""
        SELECT COUNT(r)
        FROM Review r
        WHERE r.listing.id = :listingId
        """)
    long countReviewsByListingId(@Param("listingId") UUID listingId);

    @Query("""
        SELECT new com.servicehomes.api.reviews.domain.ListingReviewSummary(
            r.listing.id,
            AVG(r.rating),
            COUNT(r)
        )
        FROM Review r
        WHERE r.listing.id IN :listingIds
        GROUP BY r.listing.id
        """)
    List<ListingReviewSummary> findSummariesByListingIds(@Param("listingIds") Collection<UUID> listingIds);
}
