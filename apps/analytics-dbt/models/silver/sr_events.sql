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
  COALESCE(TRY_TO_TIMESTAMP_NTZ(payload:timestamp::string), created_at) as event_timestamp,
  CAST(COALESCE(TRY_TO_TIMESTAMP_NTZ(payload:timestamp::string), created_at) AS DATE) as event_date,
  created_at as exported_at,
  source_file,
  source_row_number
FROM (
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
    ROW_NUMBER() OVER (
      PARTITION BY event_id
      ORDER BY created_at DESC, source_file DESC, source_row_number DESC
    ) as dedup_rank
  FROM {{ ref('br_events') }}
)
WHERE dedup_rank = 1
