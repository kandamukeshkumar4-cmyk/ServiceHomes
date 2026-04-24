{{
  config(
    materialized='view',
    schema='silver'
  )
}}

SELECT
  event_id,
  event_type,
  event_timestamp,
  event_date,
  aggregate_id as wishlist_id,
  payload:userId::string as user_id,
  payload:ownerId::string as owner_id,
  payload:listingId::string as listing_id,
  payload:itemId::string as item_id,
  payload:sourcePage::string as source_page,
  payload:shareType::string as share_type,
  payload:itemCount::int as item_count
FROM {{ ref('sr_events') }}
WHERE event_type IN ('wishlist_item_added', 'wishlist_item_removed', 'wishlist_item_reordered', 'wishlist_shared')
