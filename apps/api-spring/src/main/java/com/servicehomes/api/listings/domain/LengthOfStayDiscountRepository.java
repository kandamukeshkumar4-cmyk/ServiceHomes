package com.servicehomes.api.listings.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LengthOfStayDiscountRepository extends JpaRepository<LengthOfStayDiscount, UUID> {
    List<LengthOfStayDiscount> findByListingIdOrderByMinNightsAsc(UUID listingId);

    Optional<LengthOfStayDiscount> findByIdAndListingId(UUID id, UUID listingId);

    void deleteByIdAndListingId(UUID id, UUID listingId);
}
