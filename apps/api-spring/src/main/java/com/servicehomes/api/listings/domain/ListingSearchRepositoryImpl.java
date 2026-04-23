package com.servicehomes.api.listings.domain;

import com.servicehomes.api.listings.application.dto.ListingSearchRow;
import com.servicehomes.api.listings.application.dto.SearchListingsRequest;
import com.servicehomes.api.listings.application.dto.SearchSort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

@Repository
public class ListingSearchRepositoryImpl implements ListingSearchRepositoryCustom {

    private static final RowMapper<ListingSearchRow> ROW_MAPPER = new ListingSearchRowMapper();

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public ListingSearchRepositoryImpl(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Page<ListingSearchRow> search(SearchListingsRequest filters, Pageable pageable) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        StringBuilder fromClause = new StringBuilder("""
            FROM listings l
            JOIN listing_locations loc ON loc.listing_id = l.id
            JOIN listing_categories c ON c.id = l.category_id
            LEFT JOIN listing_policies policy ON policy.listing_id = l.id
            LEFT JOIN LATERAL (
                SELECT p.url
                FROM listing_photos p
                WHERE p.listing_id = l.id
                ORDER BY p.is_cover DESC, p.order_index ASC, p.created_at ASC
                LIMIT 1
            ) cover ON TRUE
            WHERE l.status = 'PUBLISHED'
            """);

        appendWhere(filters, params, fromClause);

        String selectClause = """
            SELECT
                l.id,
                l.title,
                cover.url AS cover_url,
                loc.city,
                loc.country,
                l.nightly_price,
                c.name AS category_name,
                loc.latitude,
                loc.longitude,
                l.max_guests,
                l.bedrooms,
                l.beds,
                l.bathrooms,
                CASE
                    WHEN :latitude IS NOT NULL AND :longitude IS NOT NULL AND loc.latitude IS NOT NULL AND loc.longitude IS NOT NULL
                    THEN earth_distance(
                        ll_to_earth(loc.latitude, loc.longitude),
                        ll_to_earth(:latitude, :longitude)
                    ) / 1000.0
                    ELSE NULL
                END AS distance_km
            """;

        StringBuilder query = new StringBuilder(selectClause).append(fromClause);
        query.append(buildOrderBy(filters));
        query.append(" LIMIT :limit OFFSET :offset");
        params.addValue("limit", pageable.getPageSize());
        params.addValue("offset", pageable.getOffset());

        List<ListingSearchRow> content = jdbcTemplate.query(query.toString(), params, ROW_MAPPER);
        long total = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) " + fromClause,
            params,
            Long.class
        );
        return new PageImpl<>(content, pageable, total);
    }

    private void appendWhere(SearchListingsRequest filters, MapSqlParameterSource params, StringBuilder sql) {
        params.addValue("latitude", filters.latitude());
        params.addValue("longitude", filters.longitude());

        if (filters.listingId() != null) {
            sql.append(" AND l.id = :listingId");
            params.addValue("listingId", filters.listingId());
        }

        if (hasText(filters.locationQuery())) {
            sql.append("""
                 AND (
                     l.search_vector @@ plainto_tsquery('english', :queryText)
                     OR LOWER(loc.city) LIKE :locationPattern
                     OR LOWER(loc.country) LIKE :locationPattern
                 )
                """);
            params.addValue("queryText", filters.locationQuery().trim());
            params.addValue("locationPattern", "%" + filters.locationQuery().trim().toLowerCase() + "%");
        }

        if (filters.categoryId() != null) {
            sql.append(" AND l.category_id = :categoryId");
            params.addValue("categoryId", filters.categoryId());
        }

        if (filters.guests() != null) {
            sql.append(" AND l.max_guests >= :guests");
            params.addValue("guests", filters.guests());
        }

        if (filters.bedrooms() != null) {
            sql.append(" AND l.bedrooms >= :bedrooms");
            params.addValue("bedrooms", filters.bedrooms());
        }

        if (filters.minPrice() != null) {
            sql.append(" AND l.nightly_price >= :minPrice");
            params.addValue("minPrice", filters.minPrice());
        }

        if (filters.maxPrice() != null) {
            sql.append(" AND l.nightly_price <= :maxPrice");
            params.addValue("maxPrice", filters.maxPrice());
        }

        if (filters.propertyTypes() != null && !filters.propertyTypes().isEmpty()) {
            sql.append(" AND l.property_type IN (:propertyTypes)");
            params.addValue("propertyTypes", filters.propertyTypes());
        }

        if (Boolean.TRUE.equals(filters.instantBook())) {
            sql.append(" AND policy.instant_book = TRUE");
        }

        if (filters.amenityIds() != null && !filters.amenityIds().isEmpty()) {
            sql.append("""
                 AND l.id IN (
                     SELECT lal.listing_id
                     FROM listing_amenity_links lal
                     WHERE lal.amenity_id IN (:amenityIds)
                 )
                """);
            params.addValue("amenityIds", filters.amenityIds());
        }

        if (filters.checkIn() != null && filters.checkOut() != null) {
            sql.append("""
                 AND NOT EXISTS (
                     SELECT 1
                     FROM reservations r
                     WHERE r.listing_id = l.id
                       AND r.status IN ('PENDING', 'CONFIRMED')
                       AND r.check_in < :checkOut
                       AND r.check_out > :checkIn
                 )
                """);
            params.addValue("checkIn", filters.checkIn());
            params.addValue("checkOut", filters.checkOut());
        }

        if (filters.latitude() != null && filters.longitude() != null && filters.radiusKm() != null) {
            sql.append("""
                 AND loc.latitude IS NOT NULL
                 AND loc.longitude IS NOT NULL
                 AND earth_distance(
                     ll_to_earth(loc.latitude, loc.longitude),
                     ll_to_earth(:latitude, :longitude)
                 ) < :radiusMeters
                """);
            params.addValue("radiusMeters", BigDecimal.valueOf(filters.radiusKm() * 1000.0d));
        }

        if (filters.swLat() != null && filters.swLng() != null && filters.neLat() != null && filters.neLng() != null) {
            sql.append("""
                 AND loc.latitude BETWEEN :swLat AND :neLat
                 AND loc.longitude BETWEEN :swLng AND :neLng
                """);
            params.addValue("swLat", filters.swLat());
            params.addValue("swLng", filters.swLng());
            params.addValue("neLat", filters.neLat());
            params.addValue("neLng", filters.neLng());
        }
    }

    private String buildOrderBy(SearchListingsRequest filters) {
        SearchSort sort = filters.sort();
        if (sort == null) {
            if (filters.latitude() != null && filters.longitude() != null) {
                return " ORDER BY distance_km ASC NULLS LAST, l.created_at DESC";
            }
            sort = hasText(filters.locationQuery()) ? SearchSort.RELEVANCE : SearchSort.NEWEST;
        }

        return switch (sort) {
            case RELEVANCE -> hasText(filters.locationQuery())
                ? " ORDER BY ts_rank(l.search_vector, plainto_tsquery('english', :queryText)) DESC, l.created_at DESC"
                : " ORDER BY l.created_at DESC";
            case PRICE_ASC -> " ORDER BY l.nightly_price ASC, l.created_at DESC";
            case PRICE_DESC -> " ORDER BY l.nightly_price DESC, l.created_at DESC";
            case NEWEST -> " ORDER BY l.created_at DESC";
        };
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static final class ListingSearchRowMapper implements RowMapper<ListingSearchRow> {

        @Override
        public ListingSearchRow mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new ListingSearchRow(
                rs.getObject("id", UUID.class),
                rs.getString("title"),
                rs.getString("cover_url"),
                rs.getString("city"),
                rs.getString("country"),
                rs.getBigDecimal("nightly_price"),
                rs.getString("category_name"),
                rs.getObject("latitude", Double.class),
                rs.getObject("longitude", Double.class),
                rs.getInt("max_guests"),
                rs.getInt("bedrooms"),
                rs.getInt("beds"),
                rs.getInt("bathrooms"),
                rs.getObject("distance_km", Double.class)
            );
        }
    }
}
