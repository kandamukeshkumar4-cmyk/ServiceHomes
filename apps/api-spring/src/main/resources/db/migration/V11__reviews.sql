CREATE TABLE IF NOT EXISTS reviews (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reservation_id UUID NOT NULL UNIQUE REFERENCES reservations(id) ON DELETE CASCADE,
    listing_id UUID NOT NULL REFERENCES listings(id) ON DELETE CASCADE,
    guest_id UUID NOT NULL REFERENCES users(id),
    rating INT NOT NULL,
    comment TEXT NOT NULL,
    host_response TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_review_rating_range CHECK (rating BETWEEN 1 AND 5)
);

CREATE INDEX IF NOT EXISTS idx_reviews_listing_created_at ON reviews(listing_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_reviews_guest_id ON reviews(guest_id);
