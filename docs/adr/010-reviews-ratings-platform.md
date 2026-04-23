# 010. Reviews and Ratings Platform

## Status

Accepted

## Context

The marketplace needs a trust loop after completed stays. Reviews must only come from completed reservations, must avoid retaliatory behavior, and must be available as a ranking signal without forcing search queries to aggregate the review table.

## Decision

Reviews are modeled as a pair of reservation-scoped records: one guest review and one optional host review. Guest reviews include overall rating plus cleanliness, accuracy, communication, location, and value ratings. Reviews remain hidden until both parties submit or the 14-day double-blind window expires.

Listing search reads cached rating fields on `listings`, including average rating, review count, category averages, and trust score. The cache is refreshed when visible reviews are created or moderated and by a scheduled maintenance refresh for timeout visibility.

Review reports are stored separately from reviews and feed an admin moderation queue. Moderation can hide or approve reviews; hidden reviews are excluded from public listing review responses and rating cache calculations.

Analytics continues to read from outbox events exported to Snowflake. The UI does not produce or own analytics aggregates. Review creation, host reciprocal reviews, host responses, reports, and moderation decisions are emitted as outbox events and modeled in dbt trust-event marts.

## Consequences

- Search can rank by review quality using indexed listing columns instead of aggregating reviews during each search.
- Public review pages only expose approved guest reviews whose double-blind visibility has opened.
- Host reviews are retained for reputation workflows but are not shown as listing reviews.
- Moderation changes require cache refreshes so public aggregates match visible approved reviews.
