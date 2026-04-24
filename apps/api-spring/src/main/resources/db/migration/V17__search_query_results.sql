ALTER TABLE search_queries
    ADD COLUMN IF NOT EXISTS result_listing_ids JSONB;
