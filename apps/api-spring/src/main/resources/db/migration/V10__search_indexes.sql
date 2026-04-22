CREATE EXTENSION IF NOT EXISTS cube;
CREATE EXTENSION IF NOT EXISTS earthdistance;

ALTER TABLE listings
    ADD COLUMN IF NOT EXISTS search_vector tsvector
    GENERATED ALWAYS AS (
        to_tsvector('english', coalesce(title, '') || ' ' || coalesce(description, ''))
    ) STORED;

CREATE INDEX IF NOT EXISTS idx_listings_search
    ON listings USING GIN (search_vector);

CREATE INDEX IF NOT EXISTS idx_listings_price
    ON listings (nightly_price);

CREATE INDEX IF NOT EXISTS idx_listings_bedrooms
    ON listings (bedrooms);

CREATE INDEX IF NOT EXISTS idx_listings_property_type
    ON listings (property_type);

ALTER TABLE listing_locations
    ADD COLUMN IF NOT EXISTS geo_point point
    GENERATED ALWAYS AS (point(longitude, latitude)) STORED;

CREATE INDEX IF NOT EXISTS idx_listing_locations_lat_lng
    ON listing_locations (latitude, longitude);
