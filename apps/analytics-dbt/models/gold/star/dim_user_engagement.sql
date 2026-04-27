{{
  config(
    materialized='table',
    schema='gold'
  )
}}

WITH wishlist_counts AS (
  SELECT
    COALESCE(user_id, owner_id) as user_id,
    COUNT(DISTINCT wishlist_id) as engaged_wishlist_count,
    COUNT_IF(event_type = 'wishlist_item_added') as wishlist_item_add_count
  FROM {{ ref('sr_wishlist_engagement') }}
  GROUP BY 1
),
saved_search_counts AS (
  SELECT
    payload:userId::string as user_id,
    COUNT(*) as saved_search_count
  FROM {{ ref('fct_event') }}
  WHERE event_type = 'saved_search_created'
  GROUP BY 1
),
recent_view_counts AS (
  SELECT
    payload:userId::string as user_id,
    COUNT(*) as recent_view_count
  FROM {{ ref('fct_event') }}
  WHERE event_type = 'recently_viewed_recorded'
  GROUP BY 1
)

SELECT
  u.user_id,
  COALESCE(w.engaged_wishlist_count, 0) as engaged_wishlist_count,
  COALESCE(w.wishlist_item_add_count, 0) as wishlist_item_add_count,
  COALESCE(s.saved_search_count, 0) as saved_search_count,
  COALESCE(r.recent_view_count, 0) as recent_view_count
FROM {{ ref('dim_user') }} u
LEFT JOIN wishlist_counts w ON u.user_id = w.user_id
LEFT JOIN saved_search_counts s ON u.user_id = s.user_id
LEFT JOIN recent_view_counts r ON u.user_id = r.user_id
