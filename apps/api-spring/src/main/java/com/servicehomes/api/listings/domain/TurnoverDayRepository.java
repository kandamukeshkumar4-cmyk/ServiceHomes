package com.servicehomes.api.listings.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TurnoverDayRepository extends JpaRepository<TurnoverDay, UUID> {
    Optional<TurnoverDay> findByListingId(UUID listingId);

    Optional<TurnoverDay> findByIdAndListingId(UUID id, UUID listingId);

    void deleteByIdAndListingId(UUID id, UUID listingId);
}
