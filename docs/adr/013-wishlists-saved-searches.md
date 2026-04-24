# ADR 013: Wishlists, Saved Searches, and Recently Viewed

## Status
Accepted

## Context
Guests need durable ways to bookmark listings, return to prior browsing, and rerun searches without coupling analytics or persistence to UI state.

## Decisions
- Store saved search filters in PostgreSQL JSONB. Filter shape changes as search evolves, and the OLTP requirement is owner-scoped retrieval rather than relational filtering.
- Use UUID v4 share tokens. Tokens are unguessable, easy to rotate, and avoid leaking wishlist or user identifiers.
- Retain recently viewed listings for 90 days. This supports short-term revisit workflows while bounding storage of behavioral history.
- Keep collaborators as a JSONB list of guest UUIDs in the wishlist row. The current permission model only needs direct membership checks; a normalized collaborator table can be introduced if invitations or roles are added later.
- Emit outbox analytics for wishlist item changes, sharing, saved search creation, and listing views so dbt models derive engagement from backend events rather than UI state.

## Consequences
- Saved search filtering is explicit DTO data at the API edge but schemaless in storage.
- Public wishlist reads are read-only and available only through `/api/public/wishlists/share/{token}` so unauthenticated clients use the route already permitted by the security configuration. There is no public enumeration endpoint.
- Collaborators can read, add items to, reorder, and remove items from shared wishlists. Only owners can delete the wishlist, update privacy, manage collaborators, or generate share links.
- Cover photo uploads use a presigned S3 URL flow. The `POST /wishlists/{id}/cover-upload` endpoint both returns the presigned URL and immediately persists the expected public URL. An explicit `PUT /wishlists/{id}/cover-photo` finalize step remains available for clients that need it.
- Recently viewed cleanup is owned by a scheduled backend job and deletes records older than 90 days.
