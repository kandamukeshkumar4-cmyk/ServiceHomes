ALTER TABLE listings
    ADD COLUMN IF NOT EXISTS average_rating NUMERIC(3, 2),
    ADD COLUMN IF NOT EXISTS review_count BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS cleanliness_rating NUMERIC(3, 2),
    ADD COLUMN IF NOT EXISTS accuracy_rating NUMERIC(3, 2),
    ADD COLUMN IF NOT EXISTS communication_rating NUMERIC(3, 2),
    ADD COLUMN IF NOT EXISTS location_rating NUMERIC(3, 2),
    ADD COLUMN IF NOT EXISTS value_rating NUMERIC(3, 2),
    ADD COLUMN IF NOT EXISTS trust_score NUMERIC(5, 2) NOT NULL DEFAULT 0;

ALTER TABLE reviews
    DROP CONSTRAINT IF EXISTS reviews_reservation_id_key;

ALTER TABLE reviews
    ADD COLUMN IF NOT EXISTS host_id UUID REFERENCES users(id),
    ADD COLUMN IF NOT EXISTS reviewer_id UUID REFERENCES users(id),
    ADD COLUMN IF NOT EXISTS reviewer_role VARCHAR(16),
    ADD COLUMN IF NOT EXISTS cleanliness_rating INT,
    ADD COLUMN IF NOT EXISTS accuracy_rating INT,
    ADD COLUMN IF NOT EXISTS communication_rating INT,
    ADD COLUMN IF NOT EXISTS location_rating INT,
    ADD COLUMN IF NOT EXISTS value_rating INT,
    ADD COLUMN IF NOT EXISTS visible_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS moderation_status VARCHAR(32) NOT NULL DEFAULT 'APPROVED',
    ADD COLUMN IF NOT EXISTS moderated_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS moderated_by UUID REFERENCES users(id),
    ADD COLUMN IF NOT EXISTS moderation_notes TEXT,
    ADD COLUMN IF NOT EXISTS report_count INT NOT NULL DEFAULT 0;

UPDATE reviews r
SET
    host_id = COALESCE(r.host_id, l.host_id),
    reviewer_id = COALESCE(r.reviewer_id, r.guest_id),
    reviewer_role = COALESCE(r.reviewer_role, 'GUEST'),
    cleanliness_rating = COALESCE(r.cleanliness_rating, r.rating),
    accuracy_rating = COALESCE(r.accuracy_rating, r.rating),
    communication_rating = COALESCE(r.communication_rating, r.rating),
    location_rating = COALESCE(r.location_rating, r.rating),
    value_rating = COALESCE(r.value_rating, r.rating),
    visible_at = COALESCE(r.visible_at, r.created_at),
    moderation_status = COALESCE(r.moderation_status, 'APPROVED')
FROM listings l
WHERE r.listing_id = l.id;

ALTER TABLE reviews
    ALTER COLUMN host_id SET NOT NULL,
    ALTER COLUMN reviewer_id SET NOT NULL,
    ALTER COLUMN reviewer_role SET NOT NULL,
    ALTER COLUMN visible_at SET NOT NULL;

ALTER TABLE reviews
    ADD CONSTRAINT uk_reviews_reservation_reviewer_role UNIQUE (reservation_id, reviewer_role),
    ADD CONSTRAINT chk_reviews_reviewer_role CHECK (reviewer_role IN ('GUEST', 'HOST')),
    ADD CONSTRAINT chk_reviews_moderation_status CHECK (moderation_status IN ('APPROVED', 'HIDDEN')),
    ADD CONSTRAINT chk_reviews_cleanliness_rating CHECK (cleanliness_rating IS NULL OR cleanliness_rating BETWEEN 1 AND 5),
    ADD CONSTRAINT chk_reviews_accuracy_rating CHECK (accuracy_rating IS NULL OR accuracy_rating BETWEEN 1 AND 5),
    ADD CONSTRAINT chk_reviews_communication_rating CHECK (communication_rating IS NULL OR communication_rating BETWEEN 1 AND 5),
    ADD CONSTRAINT chk_reviews_location_rating CHECK (location_rating IS NULL OR location_rating BETWEEN 1 AND 5),
    ADD CONSTRAINT chk_reviews_value_rating CHECK (value_rating IS NULL OR value_rating BETWEEN 1 AND 5),
    ADD CONSTRAINT chk_reviews_guest_breakdown_required CHECK (
        reviewer_role = 'HOST'
        OR (
            cleanliness_rating IS NOT NULL
            AND accuracy_rating IS NOT NULL
            AND communication_rating IS NOT NULL
            AND location_rating IS NOT NULL
            AND value_rating IS NOT NULL
        )
    );

CREATE TABLE IF NOT EXISTS review_reports (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    review_id UUID NOT NULL REFERENCES reviews(id) ON DELETE CASCADE,
    reporter_id UUID NOT NULL REFERENCES users(id),
    reason VARCHAR(64) NOT NULL,
    details TEXT,
    status VARCHAR(32) NOT NULL DEFAULT 'OPEN',
    resolved_at TIMESTAMPTZ,
    resolved_by UUID REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_review_reports_review_reporter UNIQUE (review_id, reporter_id),
    CONSTRAINT chk_review_reports_status CHECK (status IN ('OPEN', 'RESOLVED', 'DISMISSED'))
);

CREATE INDEX IF NOT EXISTS idx_reviews_listing_visible
    ON reviews(listing_id, reviewer_role, moderation_status, visible_at DESC);

CREATE INDEX IF NOT EXISTS idx_reviews_reservation_role
    ON reviews(reservation_id, reviewer_role);

CREATE INDEX IF NOT EXISTS idx_review_reports_status_created
    ON review_reports(status, created_at DESC);

WITH rating_aggregates AS (
    SELECT
        listing_id,
        ROUND(AVG(rating)::numeric, 2) AS average_rating,
        COUNT(*) AS review_count,
        ROUND(AVG(cleanliness_rating)::numeric, 2) AS cleanliness_rating,
        ROUND(AVG(accuracy_rating)::numeric, 2) AS accuracy_rating,
        ROUND(AVG(communication_rating)::numeric, 2) AS communication_rating,
        ROUND(AVG(location_rating)::numeric, 2) AS location_rating,
        ROUND(AVG(value_rating)::numeric, 2) AS value_rating
    FROM reviews
    WHERE reviewer_role = 'GUEST'
      AND moderation_status = 'APPROVED'
      AND visible_at <= NOW()
    GROUP BY listing_id
)
UPDATE listings l
SET
    average_rating = ra.average_rating,
    review_count = ra.review_count,
    cleanliness_rating = ra.cleanliness_rating,
    accuracy_rating = ra.accuracy_rating,
    communication_rating = ra.communication_rating,
    location_rating = ra.location_rating,
    value_rating = ra.value_rating,
    trust_score = LEAST(100, ROUND((COALESCE(ra.average_rating, 0) * 16 + LEAST(ra.review_count, 20))::numeric, 2))
FROM rating_aggregates ra
WHERE l.id = ra.listing_id;
