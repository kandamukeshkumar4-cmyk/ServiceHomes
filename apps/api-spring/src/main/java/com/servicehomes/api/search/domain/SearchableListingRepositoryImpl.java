package com.servicehomes.api.search.domain;

import com.servicehomes.api.search.application.dto.SearchRequest;
import com.servicehomes.api.search.application.dto.SearchResultItem;
import com.servicehomes.api.search.application.dto.SearchSort;
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
public class SearchableListingRepositoryImpl implements SearchableListingRepositoryCustom {

    private static final RowMapper<SearchableListing> ROW_MAPPER = new SearchableListingRowMapper();
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public SearchableListingRepositoryImpl(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Page<SearchableListing> search(SearchRequest request, Pageable pageable) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        StringBuilder fromClause = new StringBuilder(
            "FROM search_listings_materialized sl WHERE sl.status = 'PUBLISHED'"
        );

        appendWhere(request, params, fromClause);

        String selectClause = """
            SELECT
                sl.id,
                sl.title,
                sl.description,
                sl.nightly_price,
                sl.max_guests,
                sl.bedrooms,
                sl.beds,
                sl.bathrooms,
                sl.property_type,
                sl.status,
                sl.average_rating,
                sl.review_count,
                sl.trust_score,
                sl.created_at,
                sl.published_at,
                sl.city,
                sl.country,
                sl.state,
                sl.address_line1,
                sl.latitude,
                sl.longitude,
                sl.category_name,
                sl.instant_book,
                sl.min_nights,
                sl.max_nights,
                sl.check_in_time,
                sl.check_out_time,
                sl.amenity_ids,
                sl.cover_url,
                sl.search_vector,
                sl.geog
            """;

        StringBuilder query = new StringBuilder(selectClause).append('\n').append(fromClause);
        query.append(buildOrderBy(request));
        query.append(" LIMIT :limit OFFSET :offset");
        params.addValue("limit", pageable.getPageSize());
        params.addValue("offset", pageable.getOffset());

        List<SearchableListing> content = jdbcTemplate.query(query.toString(), params, ROW_MAPPER);

        long total = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) " + fromClause,
            params,
            Long.class
        );

        return new PageImpl<>(content, pageable, total);
    }

    @Override
    public List<String> getSuggestions(String query, int limit) {
        String sql = """
            SELECT DISTINCT ON (suggestion) suggestion
            FROM (
                SELECT city AS suggestion, 1 AS priority
                FROM search_listings_materialized
                WHERE city % :query AND status = 'PUBLISHED'

                UNION ALL

                SELECT title AS suggestion, 2 AS priority
                FROM search_listings_materialized
                WHERE title % :query AND status = 'PUBLISHED'

                UNION ALL

                SELECT country AS suggestion, 3 AS priority
                FROM search_listings_materialized
                WHERE country % :query AND status = 'PUBLISHED'
            ) suggestions
            ORDER BY priority, suggestion
            LIMIT :limit
            """;

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("query", query.trim());
        params.addValue("limit", limit);

        return jdbcTemplate.queryForList(sql, params, String.class);
    }

    private void appendWhere(SearchRequest request, MapSqlParameterSource params, StringBuilder sql) {
        if (request.hasQuery()) {
            sql.append(" AND sl.search_vector @@ websearch_to_tsquery('english', :query)");
            params.addValue("query", request.query().trim());
        }

        if (request.categoryId() != null) {
            sql.append(" AND sl.id IN (SELECT l.id FROM listings l WHERE l.category_id = :categoryId)");
            params.addValue("categoryId", request.categoryId());
        }

        if (request.guests() != null) {
            sql.append(" AND sl.max_guests >= :guests");
            params.addValue("guests", request.guests());
        }

        if (request.bedrooms() != null) {
            sql.append(" AND sl.bedrooms >= :bedrooms");
            params.addValue("bedrooms", request.bedrooms());
        }

        if (request.beds() != null) {
            sql.append(" AND sl.beds >= :beds");
            params.addValue("beds", request.beds());
        }

        if (request.bathrooms() != null) {
            sql.append(" AND sl.bathrooms >= :bathrooms");
            params.addValue("bathrooms", request.bathrooms());
        }

        if (request.minPrice() != null) {
            sql.append(" AND sl.nightly_price >= :minPrice");
            params.addValue("minPrice", request.minPrice());
        }

        if (request.maxPrice() != null) {
            sql.append(" AND sl.nightly_price <= :maxPrice");
            params.addValue("maxPrice", request.maxPrice());
        }

        if (request.propertyTypes() != null && !request.propertyTypes().isEmpty()) {
            sql.append(" AND sl.property_type IN (:propertyTypes)");
            params.addValue("propertyTypes", request.propertyTypes());
        }

        if (Boolean.TRUE.equals(request.instantBook())) {
            sql.append(" AND sl.instant_book = TRUE");
        }

        if (request.amenityIds() != null && !request.amenityIds().isEmpty()) {
            sql.append(" AND sl.amenity_ids ?| :amenityIdStrings");
            List<String> amenityIdStrings = request.amenityIds().stream()
                .map(UUID::toString)
                .toList();
            params.addValue("amenityIdStrings", amenityIdStrings.toArray(new String[0]));
        }

        if (request.checkIn() != null && request.checkOut() != null) {
            sql.append("""
                AND NOT EXISTS (
                    SELECT 1
                    FROM reservations r
                    WHERE r.listing_id = sl.id
                      AND r.status IN ('PENDING', 'CONFIRMED')
                      AND r.check_in < :checkOut
                      AND r.check_out > :checkIn
                )
                """);
            params.addValue("checkIn", request.checkIn());
            params.addValue("checkOut", request.checkOut());
        }

        if (request.hasGeoCoords() && request.radiusKm() != null) {
            sql.append(" AND sl.geog IS NOT NULL");
            sql.append(" AND ST_DWithin(sl.geog, ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography, :radiusMeters)");
            params.addValue("latitude", request.lat());
            params.addValue("longitude", request.lng());
            params.addValue("radiusMeters", request.radiusKm() * 1000);
        }

        if (request.hasBoundingBox()) {
            sql.append(" AND sl.latitude BETWEEN :swLat AND :neLat");
            sql.append(" AND sl.longitude BETWEEN :swLng AND :neLng");
            params.addValue("swLat", request.swLat());
            params.addValue("swLng", request.swLng());
            params.addValue("neLat", request.neLat());
            params.addValue("neLng", request.neLng());
        }
    }

    private String buildOrderBy(SearchRequest request) {
        SearchSort sort = request.sort();
        if (sort == null) {
            sort = request.hasQuery() ? SearchSort.RELEVANCE : SearchSort.NEWEST;
        }

        return switch (sort) {
            case RELEVANCE -> request.hasQuery()
                ? " ORDER BY ts_rank(sl.search_vector, websearch_to_tsquery('english', :query)) DESC, sl.trust_score DESC, sl.average_rating DESC NULLS LAST, sl.review_count DESC, sl.created_at DESC"
                : " ORDER BY sl.trust_score DESC, sl.average_rating DESC NULLS LAST, sl.review_count DESC, sl.created_at DESC";
            case PRICE_ASC -> " ORDER BY sl.nightly_price ASC, sl.created_at DESC";
            case PRICE_DESC -> " ORDER BY sl.nightly_price DESC, sl.created_at DESC";
            case RATING_DESC -> " ORDER BY sl.trust_score DESC, sl.average_rating DESC NULLS LAST, sl.review_count DESC, sl.created_at DESC";
            case DISTANCE -> request.hasGeoCoords()
                ? " ORDER BY ST_Distance(sl.geog, ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography) ASC NULLS LAST, sl.trust_score DESC, sl.created_at DESC"
                : " ORDER BY sl.trust_score DESC, sl.average_rating DESC NULLS LAST, sl.created_at DESC";
            case NEWEST -> " ORDER BY sl.created_at DESC";
        };
    }

    private static final class SearchableListingRowMapper implements RowMapper<SearchableListing> {
        @Override
        public SearchableListing mapRow(ResultSet rs, int rowNum) throws SQLException {
            SearchableListing listing = new SearchableListing();
            listing.setId(rs.getObject("id", UUID.class));
            listing.setTitle(rs.getString("title"));
            listing.setDescription(rs.getString("description"));
            listing.setNightlyPrice(rs.getBigDecimal("nightly_price"));
            listing.setMaxGuests(rs.getInt("max_guests"));
            listing.setBedrooms(rs.getInt("bedrooms"));
            listing.setBeds(rs.getInt("beds"));
            listing.setBathrooms(rs.getInt("bathrooms"));
            listing.setPropertyType(rs.getString("property_type"));
            listing.setStatus(rs.getString("status"));
            listing.setAverageRating(rs.getBigDecimal("average_rating"));
            listing.setReviewCount(rs.getLong("review_count"));
            listing.setTrustScore(rs.getBigDecimal("trust_score"));
            listing.setCreatedAt(rs.getTimestamp("created_at").toInstant());
            listing.setPublishedAt(rs.getTimestamp("published_at") != null ? rs.getTimestamp("published_at").toInstant() : null);
            listing.setCity(rs.getString("city"));
            listing.setCountry(rs.getString("country"));
            listing.setState(rs.getString("state"));
            listing.setAddressLine1(rs.getString("address_line1"));
            listing.setLatitude(rs.getObject("latitude", Double.class));
            listing.setLongitude(rs.getObject("longitude", Double.class));
            listing.setCategoryName(rs.getString("category_name"));
            listing.setInstantBook(rs.getObject("instant_book", Boolean.class));
            listing.setMinNights(rs.getObject("min_nights", Integer.class));
            listing.setMaxNights(rs.getObject("max_nights", Integer.class));
            listing.setCheckInTime(rs.getObject("check_in_time", java.sql.Time.class) != null
                ? rs.getObject("check_in_time", java.sql.Time.class).toLocalTime() : null);
            listing.setCheckOutTime(rs.getObject("check_out_time", java.sql.Time.class) != null
                ? rs.getObject("check_out_time", java.sql.Time.class).toLocalTime() : null);
            listing.setAmenityIds(rs.getString("amenity_ids"));
            listing.setCoverUrl(rs.getString("cover_url"));
            return listing;
        }
    }
}
