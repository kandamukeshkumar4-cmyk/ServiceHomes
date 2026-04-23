# ADR 008: Dashboard Aggregates from OLTP

## Status
Accepted

## Context
The Host and Guest dashboards require aggregated data:
- Host: reservation pipeline, occupancy rate, mock earnings, listing performance, unread messages
- Guest: upcoming/past trips, saved listings count, unread messages

We need to decide where to compute these aggregates: directly from the operational PostgreSQL database (OLTP) or from the analytics warehouse (Snowflake/dbt).

## Decision
For v1, all dashboard aggregates are computed directly from OLTP queries in the `HostDashboardService` and `GuestDashboardService`.

## Rationale
1. **Latency**: Dashboards require sub-second response times. OLTP queries with proper indexes meet this requirement.
2. **Complexity**: The aggregates are relatively simple (counts, sums, date-range filters). They do not require warehouse-level compute.
3. **Data freshness**: OLTP data is real-time. A warehouse would introduce minutes/hours of delay.
4. **Infrastructure**: The analytics pipeline (outbox → S3 → Snowflake → dbt) is not yet running in production. Relying on it would block dashboard delivery.

## Consequences
- **Positive**: Fast to implement, no additional infrastructure, real-time data.
- **Negative**: As the reservation and message volumes grow, some dashboard queries may become slow. The host dashboard in particular fetches all host reservations into memory to compute occupancy and earnings.
- **Mitigation**: The `findByHostId` query is paginated in the repository but the service currently loads all pages via `Pageable.unpaged()`. If a host has thousands of reservations, this will need to be replaced with targeted native SQL aggregate queries.

## Future Work
In Phase 7 (Analytics & BI), we will introduce pre-aggregated dbt models in the gold layer:
- `host_daily_metrics` — bookings, earnings, occupancy by host and day
- `guest_trip_summary` — upcoming/past trip counts by guest

Once these models are available and refreshed on a schedule acceptable for dashboards (e.g., every 15 minutes), we can evaluate migrating dashboard aggregates to read from the warehouse via a read-replica or direct Snowflake query. This is tracked as a future architectural decision.

## Related
- `HostDashboardService.java`
- `GuestDashboardService.java`
- ADR 005: Analytics pipeline design (outbox → S3 → Snowflake)
