{{
  config(
    materialized='table',
    schema='gold'
  )
}}

SELECT
  event_id as listing_event_sk,
  event_id,
  event_type,
  COALESCE(payload:listingId::string, aggregate_id) as listing_id,
  payload:hostId::string as host_id,
  payload:guestId::string as guest_id,
  payload:category::string as category,
  payload:ruleCount::int as rule_count,
  event_date_sk,
  event_timestamp
FROM {{ ref('fct_event') }}
WHERE event_type IN (
  'listing_viewed',
  'listing_created',
  'listing_updated',
  'listing_availability_updated',
  'listing_published',
  'listing_unpublished',
  'listing_saved',
  'listing_unsaved'
)
