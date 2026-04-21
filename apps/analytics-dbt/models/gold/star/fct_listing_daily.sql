{{
  config(
    materialized='table',
    schema='gold'
  )
}}

WITH date_spine AS (
  SELECT DATEADD(day, SEQ4(), '2020-01-01') as date_day
  FROM TABLE(GENERATOR(ROWCOUNT => 3650))
),
listing_days AS (
  SELECT
    l.id as listing_id,
    d.date_day
  FROM {{ ref('sr_listings') }} l
  CROSS JOIN date_spine d
  WHERE d.date_day BETWEEN l.created_at AND CURRENT_DATE
),
reservations_agg AS (
  SELECT
    listing_id,
    check_in as date_day,
    COUNT(*) as reservation_count,
    SUM(total_amount) as expected_booking_value
  FROM {{ ref('sr_reservations') }}
  WHERE status IN ('PENDING', 'CONFIRMED')
  GROUP BY 1, 2
)

SELECT
  ld.listing_id,
  ld.date_day,
  TO_CHAR(ld.date_day, 'YYYYMMDD')::int as date_sk,
  COALESCE(ra.reservation_count, 0) as reservation_count,
  COALESCE(ra.expected_booking_value, 0) as expected_booking_value
FROM listing_days ld
LEFT JOIN reservations_agg ra
  ON ld.listing_id = ra.listing_id
  AND ld.date_day = ra.date_day
