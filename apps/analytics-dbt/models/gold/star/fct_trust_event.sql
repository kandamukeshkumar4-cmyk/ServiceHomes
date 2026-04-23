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
  payload:reporterId::string as reporter_id,
  payload:moderatorId::string as moderator_id,
  payload:rating::int as rating,
  payload:cleanlinessRating::int as cleanliness_rating,
  payload:accuracyRating::int as accuracy_rating,
  payload:communicationRating::int as communication_rating,
  payload:locationRating::int as location_rating,
  payload:valueRating::int as value_rating,
  payload:commentLength::int as comment_length,
  payload:responseLength::int as response_length,
  payload:reason::string as report_reason,
  payload:reportCount::int as report_count,
  payload:moderationStatus::string as moderation_status,
  TRY_TO_TIMESTAMP_NTZ(payload:visibleAt::string) as visible_at,
  LOWER(payload:hasAvatar::string) = 'true' as has_avatar,
  payload:languagesCount::int as languages_count,
  LOWER(payload:emailVerified::string) = 'true' as email_verified,
  event_date_sk,
  event_timestamp
FROM {{ ref('fct_event') }}
WHERE event_type IN (
  'review_created',
  'host_review_created',
  'host_response_added',
  'review_reported',
  'review_moderated',
  'profile_updated',
  'became_host'
)
