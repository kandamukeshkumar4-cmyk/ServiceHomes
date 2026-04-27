# Wishlist Feature Overview

The guest wishlist platform adds three revisit workflows:

- Wishlists: guests create lists, add listings with notes, reorder items, invite collaborator UUIDs, and generate public read-only share links.
- Saved searches: guests persist filter JSON with location metadata and a result-count snapshot for later search execution.
- Recently viewed: listing views are upserted per user/listing and exposed as the latest 20 unique listings.

Backend storage lives in PostgreSQL through `V16__wishlists_platform.sql`. Wishlist cover uploads use S3 presigned PUT URLs with a 2MB JPEG/PNG limit and the key pattern `wishlists/{wishlistId}/cover-{timestamp}.{ext}`.

Analytics events are published through the existing outbox and normalized by the wishlist and saved-search dbt models.
