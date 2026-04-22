package com.servicehomes.api.listings.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface ListingRepository extends JpaRepository<Listing, UUID> {

    Page<Listing> findByHostId(UUID hostId, Pageable pageable);

    @Query("""
        SELECT COUNT(r) FROM Reservation r
        WHERE r.listing.id = :listingId
          AND r.status IN ('PENDING', 'CONFIRMED')
          AND r.checkIn < :checkOut
          AND r.checkOut > :checkIn
        """)
    long countOverlappingReservations(@Param("listingId") UUID listingId,
                                      @Param("checkIn") LocalDate checkIn,
                                      @Param("checkOut") LocalDate checkOut);
}
