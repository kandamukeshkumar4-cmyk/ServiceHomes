CREATE TABLE IF NOT EXISTS saved_listings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    guest_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    listing_id UUID NOT NULL REFERENCES listings(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_saved_listings_guest_listing UNIQUE (guest_id, listing_id)
);

CREATE INDEX IF NOT EXISTS idx_saved_listings_guest_created
    ON saved_listings(guest_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_saved_listings_listing_guest
    ON saved_listings(listing_id, guest_id);
