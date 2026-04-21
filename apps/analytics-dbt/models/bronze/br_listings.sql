{{
  config(
    materialized='table',
    schema='bronze'
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
FROM {{ source('app', 'listings') }}
