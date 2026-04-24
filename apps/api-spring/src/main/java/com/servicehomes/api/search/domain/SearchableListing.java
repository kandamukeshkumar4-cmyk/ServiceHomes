package com.servicehomes.api.search.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Subselect;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Immutable
@Subselect("""
    SELECT
        id, title, description, nightly_price, max_guests, bedrooms, beds, bathrooms,
        property_type, status, average_rating, review_count, trust_score, created_at, published_at,
        city, country, state, address_line1, latitude, longitude, category_name,
        instant_book, min_nights, max_nights, check_in_time, check_out_time,
        amenity_ids, cover_url, search_vector, geog
    FROM search_listings_materialized
    """)
@Getter
@Setter
@NoArgsConstructor
public class SearchableListing {

    @Id
    private UUID id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "nightly_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal nightlyPrice;

    @Column(name = "max_guests", nullable = false)
    private int maxGuests;

    @Column(nullable = false)
    private int bedrooms;

    @Column(nullable = false)
    private int beds;

    @Column(nullable = false)
    private int bathrooms;

    @Column(name = "property_type", nullable = false, length = 32)
    private String propertyType;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(name = "average_rating", precision = 3, scale = 2)
    private BigDecimal averageRating;

    @Column(name = "review_count", nullable = false)
    private long reviewCount;

    @Column(name = "trust_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal trustScore;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(length = 128)
    private String city;

    @Column(length = 128)
    private String country;

    @Column(length = 128)
    private String state;

    @Column(name = "address_line1", length = 255)
    private String addressLine1;

    private Double latitude;

    private Double longitude;

    @Column(name = "category_name", length = 64)
    private String categoryName;

    @Column(name = "instant_book")
    private Boolean instantBook;

    @Column(name = "min_nights")
    private Integer minNights;

    @Column(name = "max_nights")
    private Integer maxNights;

    @Column(name = "check_in_time")
    private LocalTime checkInTime;

    @Column(name = "check_out_time")
    private LocalTime checkOutTime;

    @Column(name = "amenity_ids", columnDefinition = "jsonb")
    private String amenityIds;

    @Column(name = "cover_url", length = 500)
    private String coverUrl;
}
