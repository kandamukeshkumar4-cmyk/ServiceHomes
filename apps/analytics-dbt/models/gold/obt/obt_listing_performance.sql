{{
  config(
    materialized='table',
    schema='gold'
  )
}}

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
  h.id as host_id,
  h.email as host_email,
  COUNT(DISTINCT r.id) as total_reservations,
  COUNT(DISTINCT CASE WHEN r.status = 'CONFIRMED' THEN r.id END) as confirmed_reservations,
  SUM(r.total_amount) as total_expected_value,
  AVG(r.total_amount) as avg_reservation_value
FROM {{ ref('sr_listings') }} l
LEFT JOIN {{ ref('dim_location') }} loc ON l.id = loc.listing_id
LEFT JOIN {{ ref('dim_host') }} h ON l.host_id = h.user_id
LEFT JOIN {{ ref('fct_reservation') }} r ON l.id = r.listing_id
GROUP BY 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14
