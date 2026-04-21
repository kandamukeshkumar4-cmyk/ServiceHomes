CREATE EXTENSION IF NOT EXISTS btree_gist;

ALTER TABLE reservations
ADD CONSTRAINT no_overlapping_reservations
EXCLUDE USING gist (
    listing_id WITH =,
    daterange(check_in, check_out, '[]') WITH &&
)
WHERE (status IN ('PENDING', 'CONFIRMED'));
