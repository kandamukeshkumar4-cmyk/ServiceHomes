# Event Schemas

Avro/JSON schemas for analytics events.

## Events

- `listing_viewed.avsc` — traveler views a listing
- `listing_created.avsc` — host creates a listing
- `listing_published.avsc` — host publishes a listing
- `reservation_created.avsc` — traveler creates a reservation
- `reservation_cancelled.avsc` — reservation is cancelled

## Outbox Table

Events are written to PostgreSQL `outbox_events` table and exported to S3 by a scheduled job.
