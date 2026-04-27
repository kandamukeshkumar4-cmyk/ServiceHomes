{{
  config(
    materialized='table',
    schema='gold'
  )
}}

SELECT
  event_id as saved_search_activity_sk,
  event_id,
  event_timestamp as saved_at,
  payload:userId::string as user_id,
  payload:searchHash::string as search_hash,
  payload:hasLocation::boolean as has_location,
  payload:hasDates::boolean as has_dates,
  payload:filterCount::int as filter_count
FROM {{ ref('fct_event') }}
WHERE event_type = 'saved_search_created'
