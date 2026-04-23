# ServiceHomes Analytics

## Setup

```bash
cd apps/analytics-dbt
pip install dbt-snowflake
dbt deps
dbt build
```

## Models

### Bronze (`models/bronze/`)
Raw ingestion from app OLTP tables and S3 events.

- `br_events` — raw outbox events from S3
- `br_users` — raw users
- `br_listings` — raw listings
- `br_reservations` — raw reservations

### Silver (`models/silver/`)
Cleaned, deduped, validated views.

- `sr_events` — deduped events
- `sr_users` — validated users
- `sr_listings` — validated listings
- `sr_reservations` — validated reservations

### Gold Star Schema (`models/gold/star/`)

- `dim_user` — travelers
- `dim_host` — hosts
- `dim_location` — listing locations
- `dim_date` — date dimension
- `fct_event` — canonical deduplicated outbox event ledger
- `fct_reservation` — reservation facts
- `fct_search` — search funnel facts from `search_executed`
- `fct_listing_event` — listing lifecycle, view, save, and availability events
- `fct_reservation_event` — reservation create, confirm, decline, and cancellation events
- `fct_trust_event` — review, host response, profile update, and host conversion events
- `fct_listing_daily` — daily listing metrics

### Gold OBT (`models/gold/obt/`)

- `obt_listing_performance` — one big table for listing analytics with reservation value and event-derived engagement counts

Dashboard view events are retained in `fct_event` for lineage, but dashboard-specific aggregate marts are intentionally not modeled here. Dashboard aggregates currently read from OLTP for freshness.

### Snapshots (`snapshots/`)

- `dim_listing_scd2` — slowly changing dimension for listings

## Tests

Run tests with:
```bash
dbt test
```

## Lineage

bronze → silver → gold
