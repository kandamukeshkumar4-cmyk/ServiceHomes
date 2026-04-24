# Event Schemas

Avro/JSON schemas for analytics events.

## Events

- `listing_viewed.avsc` — traveler views a listing
- `recently_viewed_recorded.avsc` — authenticated guest records a listing in recently viewed history
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
- `review_created.avsc` — guest leaves a review after an eligible stay
- `host_review_created.avsc` — host leaves the reciprocal double-blind review
- `host_response_added.avsc` — host posts a response to a listing review
- `review_reported.avsc` — user reports a visible review for moderation
- `review_moderated.avsc` — admin applies a review moderation decision
- `listing_saved.avsc` — traveler saves a listing for later
- `listing_unsaved.avsc` — traveler removes a saved listing
- `profile_updated.avsc` — traveler or host updates their profile details
- `became_host.avsc` — traveler account is promoted to include host capabilities
- `wishlist_item_added.avsc` — guest adds a listing to a wishlist
- `wishlist_item_removed.avsc` — guest removes a listing from a wishlist
- `wishlist_item_reordered.avsc` — guest reorders wishlist items
- `wishlist_shared.avsc` — owner shares a wishlist publicly or with collaborators
- `saved_search_created.avsc` — guest saves a search filter snapshot

## Outbox Table

Events are written to PostgreSQL `outbox_events` table and exported to S3 by a scheduled job.
