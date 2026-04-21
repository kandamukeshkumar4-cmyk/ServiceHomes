{{
  config(
    materialized='table',
    schema='bronze'
  )
}}

SELECT
  id,
  auth0_id,
  email,
  email_verified,
  created_at,
  updated_at
FROM {{ source('app', 'users') }}
