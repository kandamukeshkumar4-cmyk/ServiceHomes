{{
  config(
    materialized='table',
    schema='gold'
  )
}}

SELECT
  event_id as notification_engagement_sk,
  event_id,
  notification_id,
  notification_type,
  channel,
  engagement_type,
  recipient_user_id,
  sender_user_id,
  thread_id,
  message_id,
  reservation_id,
  listing_id,
  TO_CHAR(event_date, 'YYYYMMDD')::int as event_date_sk,
  event_timestamp as engaged_at,
  exported_at
FROM {{ ref('sr_notifications') }}
WHERE event_type = 'notification_engagement_recorded'
  AND engagement_type IS NOT NULL
