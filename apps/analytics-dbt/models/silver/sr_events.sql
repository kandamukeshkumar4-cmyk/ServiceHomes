{{
  config(
    materialized='view',
    schema='silver'
  )
}}

SELECT
  event_id,
  event_type,
  event_version,
  aggregate_type,
  aggregate_id,
  payload,
  metadata,
  created_at,
  source_file,
  source_row_number,
  ROW_NUMBER() OVER (PARTITION BY event_id ORDER BY created_at DESC) as dedup_rank
FROM {{ ref('br_events') }}
