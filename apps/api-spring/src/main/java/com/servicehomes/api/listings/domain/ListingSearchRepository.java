package com.servicehomes.api.listings.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface ListingSearchRepository extends JpaRepository<Listing, UUID> {

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
          AND (:categoryId IS NULL OR c.id = :categoryId)
          AND (:guests IS NULL OR l.maxGuests >= :guests)
          AND (:minPrice IS NULL OR l.nightlyPrice >= :minPrice)
          AND (:maxPrice IS NULL OR l.nightlyPrice <= :maxPrice)
          AND (:locationQuery IS NULL OR
               LOWER(loc.city) LIKE LOWER(CONCAT('%', :locationQuery, '%')) OR
               LOWER(loc.country) LIKE LOWER(CONCAT('%', :locationQuery, '%')))
          AND (:swLat IS NULL OR loc.latitude BETWEEN :swLat AND :neLat)
          AND (:swLng IS NULL OR loc.longitude BETWEEN :swLng AND :neLng)
          AND (:amenityIdsEmpty = true OR l.id IN (
              SELECT l2.id FROM Listing l2 JOIN l2.amenities a WHERE a.id IN :amenityIds
          ))
          AND NOT EXISTS (
              SELECT 1 FROM Reservation r
              WHERE r.listing.id = l.id
                AND r.status IN ('PENDING', 'CONFIRMED')
                AND r.checkIn < :checkOut
                AND r.checkOut > :checkIn
          )
        ORDER BY l.createdAt DESC
        """)
    List<com.servicehomes.api.listings.application.dto.ListingCardDto> search(
        @Param("locationQuery") String locationQuery,
        @Param("categoryId") UUID categoryId,
        @Param("guests") Integer guests,
        @Param("checkIn") LocalDate checkIn,
        @Param("checkOut") LocalDate checkOut,
        @Param("minPrice") BigDecimal minPrice,
        @Param("maxPrice") BigDecimal maxPrice,
        @Param("amenityIds") List<UUID> amenityIds,
        @Param("amenityIdsEmpty") boolean amenityIdsEmpty,
        @Param("swLat") Double swLat,
        @Param("swLng") Double swLng,
        @Param("neLat") Double neLat,
        @Param("neLng") Double neLng
    );
}
