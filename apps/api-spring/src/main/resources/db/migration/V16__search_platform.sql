CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE TABLE IF NOT EXISTS search_queries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id),
    query_hash VARCHAR(64) NOT NULL,
    query_text VARCHAR(500),
    filters_used JSONB,
    result_count INT NOT NULL DEFAULT 0,
    response_time_ms INT,
    geo_center_lat DOUBLE PRECISION,
    geo_center_lng DOUBLE PRECISION,
    radius_km DOUBLE PRECISION,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_search_queries_user_created
    ON search_queries(user_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_search_queries_created
    ON search_queries(created_at DESC);

CREATE INDEX IF NOT EXISTS idx_search_queries_hash
    ON search_queries(query_hash);

CREATE TABLE IF NOT EXISTS search_clicks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    search_query_id UUID NOT NULL REFERENCES search_queries(id) ON DELETE CASCADE,
    user_id UUID REFERENCES users(id),
    listing_id UUID NOT NULL REFERENCES listings(id),
    result_position INT NOT NULL,
    device_type VARCHAR(32),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_search_clicks_query
    ON search_clicks(search_query_id);

CREATE INDEX IF NOT EXISTS idx_search_clicks_listing
    ON search_clicks(listing_id);

CREATE INDEX IF NOT EXISTS idx_search_clicks_created
    ON search_clicks(created_at DESC);

DROP MATERIALIZED VIEW IF EXISTS search_listings_materialized;

CREATE MATERIALIZED VIEW search_listings_materialized AS
SELECT
    l.id,
    l.title,
    l.description,
    l.nightly_price,
    l.max_guests,
    l.bedrooms,
    l.beds,
    l.bathrooms,
    l.property_type,
    l.status,
    l.average_rating,
    l.review_count,
    l.trust_score,
    l.created_at,
    l.published_at,
    loc.city,
    loc.country,
    loc.state,
    loc.address_line1,
    loc.latitude,
    loc.longitude,
    c.name AS category_name,
    policy.instant_book,
    policy.min_nights,
    policy.max_nights,
    policy.check_in_time,
    policy.check_out_time,
    (
        SELECT jsonb_agg(lal.amenity_id)
        FROM listing_amenity_links lal
        WHERE lal.listing_id = l.id
    ) AS amenity_ids,
    (
        SELECT p.url
        FROM listing_photos p
        WHERE p.listing_id = l.id
        ORDER BY p.is_cover DESC, p.order_index ASC, p.created_at ASC
        LIMIT 1
    ) AS cover_url,
    to_tsvector('english',
        coalesce(l.title, '') || ' ' ||
        coalesce(l.description, '') || ' ' ||
        coalesce(loc.city, '') || ' ' ||
        coalesce(loc.country, '') || ' ' ||
        coalesce(loc.state, '') || ' ' ||
        coalesce(c.name, '')
    ) AS search_vector,
    CASE
        WHEN loc.latitude IS NOT NULL AND loc.longitude IS NOT NULL
        THEN ST_SetSRID(ST_MakePoint(loc.longitude, loc.latitude), 4326)::geography
        ELSE NULL
    END AS geog
FROM listings l
JOIN listing_locations loc ON loc.listing_id = l.id
JOIN listing_categories c ON c.id = l.category_id
LEFT JOIN listing_policies policy ON policy.listing_id = l.id
WHERE l.status = 'PUBLISHED';

CREATE UNIQUE INDEX IF NOT EXISTS idx_search_mv_id
    ON search_listings_materialized (id);

CREATE INDEX IF NOT EXISTS idx_search_mv_search_vector
    ON search_listings_materialized USING GIN (search_vector);

CREATE INDEX IF NOT EXISTS idx_search_mv_geog
    ON search_listings_materialized USING GIST (geog);

CREATE INDEX IF NOT EXISTS idx_search_mv_price
    ON search_listings_materialized (nightly_price);

CREATE INDEX IF NOT EXISTS idx_search_mv_bedrooms
    ON search_listings_materialized (bedrooms);

CREATE INDEX IF NOT EXISTS idx_search_mv_property_type
    ON search_listings_materialized (property_type);

CREATE INDEX IF NOT EXISTS idx_search_mv_instant_book
    ON search_listings_materialized (instant_book);

CREATE INDEX IF NOT EXISTS idx_search_mv_status
    ON search_listings_materialized (status);

CREATE INDEX IF NOT EXISTS idx_search_mv_category
    ON search_listings_materialized (category_name);

CREATE INDEX IF NOT EXISTS idx_search_mv_city
    ON search_listings_materialized USING gin (city gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_search_mv_title_trgm
    ON search_listings_materialized USING gin (title gin_trgm_ops);

CREATE OR REPLACE FUNCTION refresh_search_listings_materialized()
RETURNS TRIGGER AS $$
BEGIN
    REFRESH MATERIALIZED VIEW CONCURRENTLY search_listings_materialized;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trigger_refresh_search_mv ON listings;
CREATE TRIGGER trigger_refresh_search_mv
    AFTER INSERT OR UPDATE OR DELETE ON listings
    FOR EACH STATEMENT
    EXECUTE FUNCTION refresh_search_listings_materialized();

DROP TRIGGER IF EXISTS trigger_refresh_search_mv_location ON listing_locations;
CREATE TRIGGER trigger_refresh_search_mv_location
    AFTER INSERT OR UPDATE OR DELETE ON listing_locations
    FOR EACH STATEMENT
    EXECUTE FUNCTION refresh_search_listings_materialized();

DROP TRIGGER IF EXISTS trigger_refresh_search_mv_policy ON listing_policies;
CREATE TRIGGER trigger_refresh_search_mv_policy
    AFTER INSERT OR UPDATE OR DELETE ON listing_policies
    FOR EACH STATEMENT
    EXECUTE FUNCTION refresh_search_listings_materialized();

DROP TRIGGER IF EXISTS trigger_refresh_search_mv_amenity ON listing_amenity_links;
CREATE TRIGGER trigger_refresh_search_mv_amenity
    AFTER INSERT OR UPDATE OR DELETE ON listing_amenity_links
    FOR EACH STATEMENT
    EXECUTE FUNCTION refresh_search_listings_materialized();
