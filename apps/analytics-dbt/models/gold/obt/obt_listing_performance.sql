{{
  config(
    materialized='table',
    schema='gold'
  )
}}

WITH reservation_metrics AS (
  SELECT
    listing_id,
    COUNT(DISTINCT reservation_id) as total_reservations,
    COUNT(DISTINCT CASE WHEN status = 'CONFIRMED' THEN reservation_id END) as confirmed_reservations,
    SUM(total_amount) as total_expected_value,
    AVG(total_amount) as avg_reservation_value
  FROM {{ ref('fct_reservation') }}
  GROUP BY 1
),
listing_event_metrics AS (
  SELECT
    listing_id,
    COUNT_IF(event_type = 'listing_viewed') as listing_view_count,
    COUNT_IF(event_type = 'listing_saved') as listing_save_count,
    COUNT_IF(event_type = 'listing_unsaved') as listing_unsave_count,
    COUNT_IF(event_type = 'listing_published') as listing_publish_count,
    COUNT_IF(event_type = 'listing_unpublished') as listing_unpublish_count,
    COUNT_IF(event_type = 'listing_availability_updated') as availability_update_count
  FROM {{ ref('fct_listing_event') }}
  GROUP BY 1
),
trust_event_metrics AS (
  SELECT
    listing_id,
    COUNT_IF(event_type = 'review_created') as review_created_count,
    COUNT_IF(event_type = 'host_review_created') as host_review_created_count,
    COUNT_IF(event_type = 'host_response_added') as host_response_count,
    COUNT_IF(event_type = 'review_reported') as review_report_count,
    COUNT_IF(event_type = 'review_moderated') as review_moderation_count
  FROM {{ ref('fct_trust_event') }}
  WHERE listing_id IS NOT NULL
  GROUP BY 1
)

SELECT
  l.id as listing_id,
  l.title,
  l.property_type,
  l.status,
  l.max_guests,
  l.bedrooms,
  l.beds,
  l.bathrooms,
  l.nightly_price,
  l.created_at as listing_created_at,
  loc.city,
  loc.state,
  loc.country,
  h.user_id as host_id,
  h.email as host_email,
  COALESCE(rm.total_reservations, 0) as total_reservations,
  COALESCE(rm.confirmed_reservations, 0) as confirmed_reservations,
  COALESCE(rm.total_expected_value, 0) as total_expected_value,
  rm.avg_reservation_value,
  COALESCE(lem.listing_view_count, 0) as listing_view_count,
  COALESCE(lem.listing_save_count, 0) as listing_save_count,
  COALESCE(lem.listing_unsave_count, 0) as listing_unsave_count,
  COALESCE(lem.listing_publish_count, 0) as listing_publish_count,
  COALESCE(lem.listing_unpublish_count, 0) as listing_unpublish_count,
  COALESCE(lem.availability_update_count, 0) as availability_update_count,
  COALESCE(tem.review_created_count, 0) as review_created_count,
  COALESCE(tem.host_review_created_count, 0) as host_review_created_count,
  COALESCE(tem.host_response_count, 0) as host_response_count,
  COALESCE(tem.review_report_count, 0) as review_report_count,
  COALESCE(tem.review_moderation_count, 0) as review_moderation_count
FROM {{ ref('sr_listings') }} l
LEFT JOIN {{ ref('dim_location') }} loc ON l.id = loc.listing_id
LEFT JOIN {{ ref('dim_host') }} h ON l.host_id = h.user_id
LEFT JOIN reservation_metrics rm ON l.id = rm.listing_id
LEFT JOIN listing_event_metrics lem ON l.id = lem.listing_id
LEFT JOIN trust_event_metrics tem ON l.id = tem.listing_id
