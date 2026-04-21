{{
  config(
    materialized='view',
    schema='silver'
  )
}}

SELECT
  id,
  host_id,
  title,
  description,
  category_id,
  property_type,
  max_guests,
  bedrooms,
  beds,
  bathrooms,
  nightly_price,
  cleaning_fee,
  service_fee,
  status,
  created_at,
  updated_at,
  published_at
FROM {{ ref('br_listings') }}
WHERE status IN ('PUBLISHED', 'UNPUBLISHED', 'DRAFT')
