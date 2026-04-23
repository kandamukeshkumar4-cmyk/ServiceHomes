package com.servicehomes.api.listings.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Entity
@Table(name = "listings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Listing {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "host_id", nullable = false)
    private UUID hostId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 5000)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private ListingCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "property_type", nullable = false, length = 32)
    private PropertyType propertyType;

    @Column(name = "max_guests", nullable = false)
    private int maxGuests;

    @Column(nullable = false)
    private int bedrooms;

    @Column(nullable = false)
    private int beds;

    @Column(nullable = false)
    private int bathrooms;

    @Column(name = "nightly_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal nightlyPrice;

    @Column(name = "cleaning_fee", precision = 10, scale = 2)
    private BigDecimal cleaningFee;

    @Column(name = "service_fee", precision = 10, scale = 2)
    private BigDecimal serviceFee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    @Builder.Default
    private Status status = Status.DRAFT;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "average_rating", precision = 3, scale = 2)
    private BigDecimal averageRating;

    @Column(name = "review_count", nullable = false)
    @Builder.Default
    private long reviewCount = 0;

    @Column(name = "cleanliness_rating", precision = 3, scale = 2)
    private BigDecimal cleanlinessRating;

    @Column(name = "accuracy_rating", precision = 3, scale = 2)
    private BigDecimal accuracyRating;

    @Column(name = "communication_rating", precision = 3, scale = 2)
    private BigDecimal communicationRating;

    @Column(name = "location_rating", precision = 3, scale = 2)
    private BigDecimal locationRating;

    @Column(name = "value_rating", precision = 3, scale = 2)
    private BigDecimal valueRating;

    @Column(name = "trust_score", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal trustScore = BigDecimal.ZERO;

    @OneToOne(mappedBy = "listing", cascade = CascadeType.ALL, orphanRemoval = true)
    private ListingLocation location;

    @OneToOne(mappedBy = "listing", cascade = CascadeType.ALL, orphanRemoval = true)
    private ListingPolicy policy;

    @OneToMany(mappedBy = "listing", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    @Builder.Default
    private List<ListingPhoto> photos = new ArrayList<>();

    @ManyToMany
    @JoinTable(
        name = "listing_amenity_links",
        joinColumns = @JoinColumn(name = "listing_id"),
        inverseJoinColumns = @JoinColumn(name = "amenity_id")
    )
    @Builder.Default
    private Set<ListingAmenity> amenities = new HashSet<>();

    @OneToMany(mappedBy = "listing", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("startDate ASC, endDate ASC")
    @Builder.Default
    private List<ListingAvailabilityRule> availabilityRules = new ArrayList<>();

    public enum PropertyType {
        APARTMENT, HOUSE, VILLA, CABIN, COTTAGE, TINY_HOME, TREEHOUSE, BOAT, CAMPER
    }

    public enum Status {
        DRAFT, PUBLISHED, UNPUBLISHED
    }
}
