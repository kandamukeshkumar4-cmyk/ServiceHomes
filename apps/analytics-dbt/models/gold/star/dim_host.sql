{{
  config(
    materialized='table',
    schema='gold'
  )
}}

SELECT
  id as host_sk,
  id as user_id,
  auth0_id,
  email,
  created_at as host_since
FROM {{ ref('sr_users') }}
WHERE id IN (SELECT DISTINCT host_id FROM {{ ref('sr_listings') }})
