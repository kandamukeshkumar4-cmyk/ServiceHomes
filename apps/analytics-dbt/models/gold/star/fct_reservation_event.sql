{{
  config(
    materialized='table',
    schema='gold'
  )
}}

SELECT
  event_id as reservation_event_sk,
  event_id,
  event_type,
  COALESCE(payload:reservationId::string, aggregate_id) as reservation_id,
  payload:listingId::string as listing_id,
  payload:guestId::string as guest_id,
  payload:status::string as status,
  payload:decisionBy::string as decision_by,
  payload:by::string as cancelled_by,
  TRY_TO_DECIMAL(payload:totalAmount::string, 12, 2) as total_amount,
  event_date_sk,
  event_timestamp
FROM {{ ref('fct_event') }}
WHERE event_type IN (
  'reservation_created',
  'reservation_confirmed',
  'reservation_declined',
  'reservation_cancelled'
)
