{{
  config(
    materialized='view',
    schema='silver'
  )
}}

SELECT
  id AS search_query_id,
  user_id,
  query_hash,
  query_text,
  filters_used,
  result_count,
  response_time_ms,
  geo_center_lat,
  geo_center_lng,
  radius_km,
  created_at AS searched_at,
  TO_CHAR(created_at, 'YYYYMMDD')::int AS search_date_sk
FROM {{ source('app', 'search_queries') }}
