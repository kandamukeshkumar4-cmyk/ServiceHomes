# Event Schemas

Avro/JSON schemas for analytics events.

## Events

- `listing_viewed.avsc` — traveler views a listing
- `listing_created.avsc` — host creates a listing
- `listing_updated.avsc` — host updates a listing
- `listing_availability_updated.avsc` — host replaces calendar availability rules
- `listing_published.avsc` — host publishes a listing
- `listing_unpublished.avsc` — host unpublishes a listing
- `search_executed.avsc` — traveler executes a search
- `reservation_created.avsc` — traveler creates a reservation
- `reservation_confirmed.avsc` — reservation is confirmed, including instant book and host acceptance
- `reservation_declined.avsc` — host declines a pending reservation request
- `reservation_cancelled.avsc` — reservation is cancelled

## Outbox Table

Events are written to PostgreSQL `outbox_events` table and exported to S3 by a scheduled job.
