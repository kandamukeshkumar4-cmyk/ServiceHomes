package com.servicehomes.api.listings.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface WeekendMultiplierRepository extends JpaRepository<WeekendMultiplier, UUID> {
    Optional<WeekendMultiplier> findByListingId(UUID listingId);

    Optional<WeekendMultiplier> findByIdAndListingId(UUID id, UUID listingId);

    void deleteByIdAndListingId(UUID id, UUID listingId);
}
