{{
  config(
    materialized='table',
    schema='gold'
  )
}}

SELECT
  r.id as reservation_sk,
  r.id as reservation_id,
  r.listing_id,
  r.guest_id,
  TO_CHAR(r.check_in, 'YYYYMMDD')::int as check_in_date_sk,
  TO_CHAR(r.check_out, 'YYYYMMDD')::int as check_out_date_sk,
  r.check_in,
  r.check_out,
  r.guests,
  r.total_nights,
  r.nightly_price,
  r.cleaning_fee,
  r.service_fee,
  r.total_amount,
  r.status,
  r.created_at
FROM {{ ref('sr_reservations') }} r
