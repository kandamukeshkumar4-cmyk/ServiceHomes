package com.servicehomes.api.wishlists.domain;

import com.servicehomes.api.listings.domain.Listing;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "recently_viewed",
    uniqueConstraints = @UniqueConstraint(name = "uk_recently_viewed_user_listing", columnNames = {"user_id", "listing_id"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecentlyViewed {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_id", nullable = false)
    private Listing listing;

    @Column(name = "viewed_at", nullable = false)
    private Instant viewedAt;

    @Column(name = "source_page", nullable = false, length = 32)
    private String sourcePage;
}
