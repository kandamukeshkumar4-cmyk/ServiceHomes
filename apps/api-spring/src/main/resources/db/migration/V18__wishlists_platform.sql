CREATE TABLE IF NOT EXISTS wishlists (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(160) NOT NULL,
    description TEXT,
    cover_photo_url TEXT,
    is_public BOOLEAN NOT NULL DEFAULT FALSE,
    share_token VARCHAR(64) UNIQUE,
    collaborator_ids JSONB NOT NULL DEFAULT '[]'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS wishlist_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    wishlist_id UUID NOT NULL REFERENCES wishlists(id) ON DELETE CASCADE,
    listing_id UUID NOT NULL REFERENCES listings(id) ON DELETE CASCADE,
    note TEXT,
    sort_order INT NOT NULL DEFAULT 0,
    added_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_wishlist_items_wishlist_listing UNIQUE (wishlist_id, listing_id)
);

CREATE TABLE IF NOT EXISTS saved_searches (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(160) NOT NULL,
    filters_json JSONB NOT NULL,
    location_query VARCHAR(240),
    geo_center_lat DOUBLE PRECISION,
    geo_center_lng DOUBLE PRECISION,
    radius_km DOUBLE PRECISION,
    notify_new_results BOOLEAN NOT NULL DEFAULT FALSE,
    result_count_snapshot INT,
    last_notified_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS recently_viewed (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    listing_id UUID NOT NULL REFERENCES listings(id) ON DELETE CASCADE,
    viewed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    source_page VARCHAR(32) NOT NULL,
    CONSTRAINT uk_recently_viewed_user_listing UNIQUE (user_id, listing_id),
    CONSTRAINT chk_recently_viewed_source CHECK (source_page IN ('search', 'wishlist', 'home'))
);

CREATE INDEX IF NOT EXISTS idx_wishlists_owner
    ON wishlists(owner_id, updated_at DESC);

CREATE INDEX IF NOT EXISTS idx_wishlists_public
    ON wishlists(is_public, updated_at DESC);

CREATE INDEX IF NOT EXISTS idx_wishlists_collaborators
    ON wishlists USING GIN (collaborator_ids);

CREATE INDEX IF NOT EXISTS idx_wishlist_items_listing
    ON wishlist_items(listing_id);

CREATE INDEX IF NOT EXISTS idx_wishlist_items_wishlist_order
    ON wishlist_items(wishlist_id, sort_order ASC, added_at ASC);

CREATE INDEX IF NOT EXISTS idx_recently_viewed_user_viewed
    ON recently_viewed(user_id, viewed_at DESC);

CREATE INDEX IF NOT EXISTS idx_saved_searches_user
    ON saved_searches(user_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_saved_searches_notifications
    ON saved_searches(notify_new_results, last_notified_at)
    WHERE notify_new_results = TRUE;
