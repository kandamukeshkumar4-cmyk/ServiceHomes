# ADR 009: Analytics Event Marts from Outbox

## Status
Accepted

## Context
ServiceHomes emits product analytics events into the PostgreSQL `outbox_events` table and exports them to S3 for Snowflake ingestion. Product analytics must not read directly from UI state, because frontend-only data would bypass backend business rules, identity resolution, and durable event export.

## Decision
Build event marts in dbt from the exported outbox event stream:
- `sr_events` deduplicates raw S3 events and assigns a stable event timestamp.
- `fct_event` is the canonical gold event ledger.
- Topical marts model search, listing engagement, reservation funnel, and trust/conversion events.
- Listing performance analytics enrich reservation metrics with event-derived engagement counts.

Dashboard-specific aggregate marts are deferred. Host and guest dashboard responses remain OLTP-backed until dashboard warehouse freshness and refresh schedules are explicitly designed.

## Consequences
- Analytics consumers use Snowflake/dbt models instead of frontend state or ad hoc UI-derived aggregates.
- Event quality checks fail fast when event IDs duplicate, event types drift, or required fact keys are missing.
- Adding a new product event requires updating event schemas and dbt accepted-value tests in the same slice.

## Related
- ADR 008: Dashboard Aggregates from OLTP
- `apps/analytics-dbt/models/silver/sr_events.sql`
- `apps/analytics-dbt/models/gold/star/fct_event.sql`
