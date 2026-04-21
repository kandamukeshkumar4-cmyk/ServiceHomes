package com.servicehomes.api.listings.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "listing_policies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ListingPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_id", nullable = false, unique = true)
    private Listing listing;

    @Column(name = "check_in_time")
    private LocalTime checkInTime;

    @Column(name = "check_out_time")
    private LocalTime checkOutTime;

    @Column(name = "min_nights", nullable = false)
    @Builder.Default
    private int minNights = 1;

    @Column(name = "max_nights")
    private Integer maxNights;

    @Enumerated(EnumType.STRING)
    @Column(name = "cancellation_policy", length = 32)
    @Builder.Default
    private CancellationPolicy cancellationPolicy = CancellationPolicy.FLEXIBLE;

    @Column(name = "instant_book", nullable = false)
    @Builder.Default
    private boolean instantBook = false;

    public enum CancellationPolicy {
        FLEXIBLE, MODERATE, STRICT
    }
}
