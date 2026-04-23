{{
  config(
    materialized='table',
    schema='gold'
  )
}}

SELECT
  event_id as trust_event_sk,
  event_id,
  event_type,
  aggregate_type,
  aggregate_id,
  payload:reviewId::string as review_id,
  payload:reservationId::string as reservation_id,
  payload:listingId::string as listing_id,
  COALESCE(payload:userId::string, payload:guestId::string, payload:hostId::string, aggregate_id) as user_id,
  payload:guestId::string as guest_id,
  payload:hostId::string as host_id,
  payload:rating::int as rating,
  payload:commentLength::int as comment_length,
  payload:responseLength::int as response_length,
  LOWER(payload:hasAvatar::string) = 'true' as has_avatar,
  payload:languagesCount::int as languages_count,
  LOWER(payload:emailVerified::string) = 'true' as email_verified,
  event_date_sk,
  event_timestamp
FROM {{ ref('fct_event') }}
WHERE event_type IN (
  'review_created',
  'host_response_added',
  'profile_updated',
  'became_host'
)
