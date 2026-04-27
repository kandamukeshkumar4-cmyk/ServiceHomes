package com.servicehomes.api.wishlists.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "saved_searches")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavedSearch {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 160)
    private String name;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "filters_json", nullable = false, columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> filtersJson = new LinkedHashMap<>();

    @Column(name = "location_query", length = 240)
    private String locationQuery;

    @Column(name = "geo_center_lat")
    private Double geoCenterLat;

    @Column(name = "geo_center_lng")
    private Double geoCenterLng;

    @Column(name = "radius_km")
    private Double radiusKm;

    @Column(name = "notify_new_results", nullable = false)
    @Builder.Default
    private boolean notifyNewResults = false;

    @Column(name = "result_count_snapshot")
    private Integer resultCountSnapshot;

    @Column(name = "last_notified_at")
    private Instant lastNotifiedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
