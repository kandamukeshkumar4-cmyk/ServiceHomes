{{
  config(
    materialized='table',
    schema='gold'
  )
}}

SELECT
  wishlist_id,
  CAST(event_timestamp AS DATE) as interaction_date,
  COUNT_IF(event_type = 'wishlist_item_added') as items_added,
  COUNT_IF(event_type = 'wishlist_item_removed') as items_removed,
  COUNT_IF(event_type = 'wishlist_item_reordered') as reorders,
  COUNT_IF(event_type = 'wishlist_shared') as shares
FROM {{ ref('sr_wishlist_engagement') }}
GROUP BY 1, 2
