{{
  config(
    materialized='table',
    schema='gold'
  )
}}

SELECT
  id as user_sk,
  id as user_id,
  auth0_id,
  email,
  email_verified,
  created_at as registration_date
FROM {{ ref('sr_users') }}
