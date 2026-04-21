{{
  config(
    materialized='view',
    schema='silver'
  )
}}

SELECT
  id,
  auth0_id,
  email,
  email_verified,
  created_at,
  updated_at
FROM {{ ref('br_users') }}
WHERE email IS NOT NULL
