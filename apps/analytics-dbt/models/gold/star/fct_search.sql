{{
  config(
    materialized='table',
    schema='gold'
  )
}}

SELECT
  event_id as search_sk,
  event_id,
  event_type,
  aggregate_id as search_id,
  event_date_sk,
  event_timestamp as searched_at,
  payload:locationQuery::string as location_query,
  payload:categoryId::string as category_id,
  payload:guests::int as guests,
  TRY_TO_DATE(payload:checkIn::string) as check_in,
  TRY_TO_DATE(payload:checkOut::string) as check_out,
  TRY_TO_DECIMAL(payload:minPrice::string, 12, 2) as min_price,
  TRY_TO_DECIMAL(payload:maxPrice::string, 12, 2) as max_price,
  payload:bedrooms::int as bedrooms,
  payload:propertyTypes as property_types,
  payload:instantBook::boolean as instant_book,
  TRY_TO_DOUBLE(payload:radiusKm::string) as radius_km,
  payload:sort::string as sort,
  COALESCE(payload:page::int, 0) as page,
  COALESCE(payload:pageSize::int, 20) as page_size,
  payload:resultCount::int as result_count
FROM {{ ref('fct_event') }}
WHERE event_type = 'search_executed'
