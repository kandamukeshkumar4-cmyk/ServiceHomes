# ServiceHomes Analytics

## Setup

```bash
cd apps/analytics-dbt
pip install dbt-snowflake
dbt deps
dbt build
```

## Models

- `bronze/` — raw ingestion from S3
- `silver/` — cleaned and tested
- `gold/` — business marts (OBT + star schema)
- `snapshots/` — SCD2 tracking

## Event sources

Events are exported from the app outbox to S3 and loaded via Snowflake stages.
