{{
  config(
    materialized='table',
    schema='gold'
  )
}}

SELECT
  event_id as notification_delivery_sk,
  event_id,
  notification_id,
  notification_type,
  channel,
  delivery_status,
  recipient_user_id,
  sender_user_id,
  thread_id,
  message_id,
  reservation_id,
  listing_id,
  dedupe_key,
  provider,
  provider_message_id,
  failure_code,
  failure_reason,
  retryable,
  attempt_number,
  TO_CHAR(event_date, 'YYYYMMDD')::int as event_date_sk,
  event_timestamp as delivery_event_at,
  CASE WHEN delivery_status = 'DELIVERED' THEN event_timestamp END as delivered_at,
  CASE WHEN delivery_status = 'FAILED' THEN event_timestamp END as failed_at,
  exported_at
FROM {{ ref('sr_notifications') }}
WHERE delivery_status IS NOT NULL
