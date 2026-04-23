{{
  config(
    materialized='table',
    schema='gold'
  )
}}

SELECT
  event_id as event_sk,
  event_id,
  event_type,
  event_version,
  aggregate_type,
  aggregate_id,
  TO_CHAR(event_date, 'YYYYMMDD')::int as event_date_sk,
  event_timestamp,
  event_date,
  payload,
  metadata,
  source_file,
  source_row_number,
  exported_at
FROM {{ ref('sr_events') }}
