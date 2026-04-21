{{
  config(
    materialized='table',
    schema='gold'
  )
}}

SELECT
  id as location_sk,
  id as location_id,
  listing_id,
  address_line1,
  city,
  state,
  country,
  latitude,
  longitude
FROM {{ source('app', 'listing_locations') }}
