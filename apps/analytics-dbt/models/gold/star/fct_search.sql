{{
  config(
    materialized='table',
    schema='gold'
  )
}}

SELECT
  id as search_sk,
  event_id,
  event_type,
  aggregate_id as listing_id,
  payload:category::string as category,
  payload:locationQuery::string as location_query,
  payload:guests::int as guests,
  created_at as searched_at
FROM {{ ref('sr_events') }}
WHERE event_type = 'search_executed'
  AND dedup_rank = 1
