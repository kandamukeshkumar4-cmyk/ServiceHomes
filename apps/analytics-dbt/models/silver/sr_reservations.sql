{{
  config(
    materialized='view',
    schema='silver'
  )
}}

SELECT
  id,
  listing_id,
  guest_id,
  check_in,
  check_out,
  guests,
  total_nights,
  nightly_price,
  cleaning_fee,
  service_fee,
  total_amount,
  status,
  created_at,
  updated_at
FROM {{ ref('br_reservations') }}
WHERE check_in < check_out
