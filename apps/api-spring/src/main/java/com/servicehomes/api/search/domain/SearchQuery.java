package com.servicehomes.api.search.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "search_queries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchQuery {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "query_hash", nullable = false, length = 64)
    private String queryHash;

    @Column(name = "query_text", length = 500)
    private String queryText;

    @Column(name = "filters_used", columnDefinition = "jsonb")
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    private String filtersUsed;

    @Column(name = "result_count", nullable = false)
    private int resultCount;

    @Column(name = "response_time_ms")
    private Integer responseTimeMs;

    @Column(name = "geo_center_lat")
    private Double geoCenterLat;

    @Column(name = "geo_center_lng")
    private Double geoCenterLng;

    @Column(name = "radius_km")
    private Double radiusKm;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
