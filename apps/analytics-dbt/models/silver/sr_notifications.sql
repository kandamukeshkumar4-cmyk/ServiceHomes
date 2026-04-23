{{
  config(
    materialized='view',
    schema='silver'
  )
}}

SELECT
  event_id,
  event_type,
  event_version,
  aggregate_type,
  aggregate_id,
  COALESCE(payload:notificationId::string, aggregate_id) as notification_id,
  payload:notificationType::string as notification_type,
  UPPER(payload:channel::string) as channel,
  COALESCE(
    payload:recipientUserId::string,
    payload:recipientId::string,
    payload:userId::string
  ) as recipient_user_id,
  payload:senderUserId::string as sender_user_id,
  payload:threadId::string as thread_id,
  payload:messageId::string as message_id,
  payload:reservationId::string as reservation_id,
  payload:listingId::string as listing_id,
  payload:dedupeKey::string as dedupe_key,
  payload:provider::string as provider,
  payload:providerMessageId::string as provider_message_id,
  payload:failureCode::string as failure_code,
  payload:failureReason::string as failure_reason,
  CASE
    WHEN LOWER(COALESCE(payload:retryable::string, 'false')) = 'true' THEN TRUE
    ELSE FALSE
  END as retryable,
  COALESCE(TRY_TO_NUMBER(payload:attemptNumber::string), 1)::int as attempt_number,
  UPPER(payload:engagementType::string) as engagement_type,
  CASE event_type
    WHEN 'notification_delivery_requested' THEN 'REQUESTED'
    WHEN 'notification_delivery_succeeded' THEN 'DELIVERED'
    WHEN 'notification_delivery_failed' THEN 'FAILED'
  END as delivery_status,
  payload,
  metadata,
  event_timestamp,
  event_date,
  exported_at,
  source_file,
  source_row_number
FROM {{ ref('sr_events') }}
WHERE event_type IN (
  'notification_delivery_requested',
  'notification_delivery_succeeded',
  'notification_delivery_failed',
  'notification_engagement_recorded'
)
