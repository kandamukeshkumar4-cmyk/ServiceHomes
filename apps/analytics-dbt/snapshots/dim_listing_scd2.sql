{% snapshot dim_listing_scd2 %}

{{
  config(
    target_schema='snapshots',
    strategy='check',
    unique_key='id',
    check_cols=['title', 'description', 'nightly_price', 'status', 'max_guests', 'bedrooms', 'beds', 'bathrooms']
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
  updated_at
FROM {{ ref('sr_listings') }}

{% endsnapshot %}
