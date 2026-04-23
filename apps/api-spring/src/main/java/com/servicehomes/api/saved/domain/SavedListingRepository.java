package com.servicehomes.api.saved.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface SavedListingRepository extends JpaRepository<SavedListing, UUID> {

    boolean existsByGuestIdAndListing_Id(UUID guestId, UUID listingId);

    void deleteByGuestIdAndListing_Id(UUID guestId, UUID listingId);

    @Query("""
        SELECT DISTINCT sl
        FROM SavedListing sl
        JOIN FETCH sl.listing l
        JOIN FETCH l.category
        LEFT JOIN FETCH l.location
        LEFT JOIN FETCH l.policy
        LEFT JOIN FETCH l.photos
        WHERE sl.guestId = :guestId
        ORDER BY sl.createdAt DESC
        """)
    List<SavedListing> findSavedListingsByGuestId(@Param("guestId") UUID guestId);

    @Query("""
        SELECT sl.listing.id
        FROM SavedListing sl
        WHERE sl.guestId = :guestId
          AND sl.listing.id IN :listingIds
        """)
    List<UUID> findListingIdsByGuestIdAndListingIdIn(
        @Param("guestId") UUID guestId,
        @Param("listingIds") Collection<UUID> listingIds
    );
}
