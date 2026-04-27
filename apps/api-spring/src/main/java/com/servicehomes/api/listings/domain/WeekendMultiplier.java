package com.servicehomes.api.listings.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "weekend_multipliers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeekendMultiplier {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_id", nullable = false)
    private Listing listing;

    @Column(name = "friday_multiplier", nullable = false, precision = 5, scale = 2)
    private BigDecimal fridayMultiplier;

    @Column(name = "saturday_multiplier", nullable = false, precision = 5, scale = 2)
    private BigDecimal saturdayMultiplier;

    @Column(name = "sunday_multiplier", nullable = false, precision = 5, scale = 2)
    private BigDecimal sundayMultiplier;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
