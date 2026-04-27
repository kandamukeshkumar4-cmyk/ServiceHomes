package com.servicehomes.api.listings.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SeasonalPricingTemplateRepository extends JpaRepository<SeasonalPricingTemplate, UUID> {
    List<SeasonalPricingTemplate> findByListingIdOrderByStartDateAsc(UUID listingId);

    List<SeasonalPricingTemplate> findByListingIdAndEndDateGreaterThanEqualAndStartDateLessThanEqual(
        UUID listingId,
        LocalDate startDate,
        LocalDate endDate
    );

    Optional<SeasonalPricingTemplate> findByIdAndListingId(UUID id, UUID listingId);

    void deleteByIdAndListingId(UUID id, UUID listingId);
}
