package com.servicehomes.api.listings.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface ListingAvailabilityRuleRepository extends JpaRepository<ListingAvailabilityRule, UUID> {
    List<ListingAvailabilityRule> findByListingIdOrderByStartDateAsc(UUID listingId);

    List<ListingAvailabilityRule> findByListingIdAndEndDateGreaterThanEqualAndStartDateLessThanEqualOrderByStartDateAsc(
        UUID listingId,
        LocalDate startDate,
        LocalDate endDate
    );
}
