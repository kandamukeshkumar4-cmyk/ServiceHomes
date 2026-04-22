package com.servicehomes.api.reservations.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, UUID> {

    @Query("""
        SELECT r FROM Reservation r
        WHERE r.guestId = :guestId
        ORDER BY r.checkIn DESC
        """)
    Page<Reservation> findByGuestId(@Param("guestId") UUID guestId, Pageable pageable);

    @Query("""
        SELECT r FROM Reservation r JOIN r.listing l
        WHERE l.hostId = :hostId
        ORDER BY r.checkIn DESC
        """)
    Page<Reservation> findByHostId(@Param("hostId") UUID hostId, Pageable pageable);

    @Query(value = """
        SELECT COUNT(*)
        FROM reservations r
        JOIN listings l ON l.id = r.listing_id
        JOIN listing_policies p ON p.listing_id = l.id
        WHERE l.host_id = :hostId
          AND p.instant_book = false
          AND (
            r.status IN ('CONFIRMED', 'DECLINED')
            OR (
              r.status = 'PENDING'
              AND r.created_at <= :referenceTime - interval '24 hours'
            )
          )
        """, nativeQuery = true)
    long countRequestsEligibleForResponseRate(@Param("hostId") UUID hostId, @Param("referenceTime") Instant referenceTime);

    @Query(value = """
        SELECT COUNT(*)
        FROM reservations r
        JOIN listings l ON l.id = r.listing_id
        JOIN listing_policies p ON p.listing_id = l.id
        WHERE l.host_id = :hostId
          AND p.instant_book = false
          AND r.status IN ('CONFIRMED', 'DECLINED')
          AND r.updated_at <= r.created_at + interval '24 hours'
        """, nativeQuery = true)
    long countRequestsRespondedWithin24Hours(@Param("hostId") UUID hostId);
}
