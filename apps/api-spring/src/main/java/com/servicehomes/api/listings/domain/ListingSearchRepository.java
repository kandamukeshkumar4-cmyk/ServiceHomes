package com.servicehomes.api.listings.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ListingSearchRepository extends JpaRepository<Listing, UUID>, ListingSearchRepositoryCustom {

    @Query("""
        SELECT new com.servicehomes.api.listings.application.dto.ListingCardDto(
            l.id, l.title,
            (SELECT p.url FROM ListingPhoto p WHERE p.listing = l AND p.isCover = true),
            loc.city, loc.country, l.nightlyPrice, c.name, loc.latitude, loc.longitude,
            l.maxGuests, l.bedrooms, l.beds, l.bathrooms
        )
        FROM Listing l
        JOIN l.location loc
        JOIN l.category c
        WHERE l.status = 'PUBLISHED'
          AND l.hostId = :hostId
        ORDER BY COALESCE(l.publishedAt, l.createdAt) DESC
        """)
    List<com.servicehomes.api.listings.application.dto.ListingCardDto> findPublishedByHostId(@Param("hostId") UUID hostId);
}
