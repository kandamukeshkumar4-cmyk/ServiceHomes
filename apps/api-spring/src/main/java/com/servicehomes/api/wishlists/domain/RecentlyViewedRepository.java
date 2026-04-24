package com.servicehomes.api.wishlists.domain;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RecentlyViewedRepository extends JpaRepository<RecentlyViewed, UUID> {
    @EntityGraph(attributePaths = {"listing", "listing.photos"})
    List<RecentlyViewed> findTop20ByUserIdOrderByViewedAtDesc(UUID userId);
    Optional<RecentlyViewed> findByUserIdAndListingId(UUID userId, UUID listingId);
    void deleteByUserId(UUID userId);
    long deleteByViewedAtBefore(Instant cutoff);

    @Modifying
    @Query(value = """
        INSERT INTO recently_viewed (id, user_id, listing_id, viewed_at, source_page)
        VALUES (:id, :userId, :listingId, :viewedAt, :sourcePage)
        ON CONFLICT (user_id, listing_id)
        DO UPDATE SET viewed_at = EXCLUDED.viewed_at, source_page = EXCLUDED.source_page
        """, nativeQuery = true)
    void upsertViewedListing(@Param("id") UUID id,
                             @Param("userId") UUID userId,
                             @Param("listingId") UUID listingId,
                             @Param("viewedAt") Instant viewedAt,
                             @Param("sourcePage") String sourcePage);
}
