package com.servicehomes.api.reservations.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
}
