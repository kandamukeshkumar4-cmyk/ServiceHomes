# ADR-002: Host Profiles and Role Promotion

## Status
Accepted

## Context
Phase 1.4 requires richer user profiles, public host pages, and a safe path for verified travelers to become hosts without splitting identity across multiple accounts.

## Decision
We will:
- keep a single `users` identity record with additive roles instead of separate traveler and host accounts
- extend `profiles` with host-facing presentation fields and a normalized `profile_languages` collection
- expose `PATCH /me/profile` for self-service profile edits and `POST /me/become-host` for role promotion
- expose `GET /hosts/{hostId}` as a public host-profile view that includes response-rate stats and published listings
- compute host response rate from OLTP reservation and listing data, excluding instant-book listings and counting only requests that are resolved or overdue after 24 hours

## Consequences
- Travelers can become hosts without losing access to guest functionality
- Public host pages can be rendered directly from the product database without needing analytics warehouse latency
- Role checks must remain additive across the app because a host can still be a traveler
- Reservation decision timestamps become part of the host reputation surface, so booking lifecycle changes must preserve accurate `created_at` and `updated_at` behavior

## Alternatives Considered
- Separate traveler and host accounts: rejected because it fragments identity and complicates reservations, messaging, and dashboards
- Warehouse-derived host stats: rejected for v1 because it adds data-latency and infra dependencies to a user-facing page
- Free-form language JSON on `profiles`: rejected in favor of a normalized collection that is easier to validate and query

## Date
2026-04-22
