package com.servicehomes.api.messaging.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MessageThreadRepository extends JpaRepository<MessageThread, UUID> {

    Optional<MessageThread> findByReservationId(UUID reservationId);

    List<MessageThread> findByGuestIdOrHostId(UUID guestId, UUID hostId);
}
