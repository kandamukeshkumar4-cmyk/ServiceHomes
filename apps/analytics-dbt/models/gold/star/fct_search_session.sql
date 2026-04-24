{{
  config(
    materialized='table',
    schema='gold'
  )
}}

WITH search_clicks AS (
    SELECT
        search_query_id,
        user_id,
        listing_id,
        result_position,
        device_type,
        created_at AS clicked_at
    FROM {{ source('app', 'search_clicks') }}
),

search_base AS (
    SELECT * FROM {{ ref('sr_searches') }}
),

click_metrics AS (
    SELECT
        search_query_id,
        COUNT(*) AS total_clicks,
        COUNT(DISTINCT listing_id) AS unique_listings_clicked,
        MIN(result_position) AS first_click_position,
        MAX(result_position) AS last_click_position
    FROM search_clicks
    GROUP BY search_query_id
),

click_details AS (
    SELECT
        sc.search_query_id,
        sc.listing_id,
        sc.result_position,
        sc.device_type,
        sc.clicked_at,
        ROW_NUMBER() OVER (PARTITION BY sc.search_query_id ORDER BY sc.clicked_at ASC) AS click_order
    FROM search_clicks sc
)

SELECT
    s.search_query_id,
    s.user_id,
    s.query_hash,
    s.query_text,
    s.filters_used,
    s.result_count,
    s.response_time_ms,
    s.geo_center_lat,
    s.geo_center_lng,
    s.radius_km,
    s.searched_at,
    s.search_date_sk,
    COALESCE(cm.total_clicks, 0) AS total_clicks,
    COALESCE(cm.unique_listings_clicked, 0) AS unique_listings_clicked,
    cm.first_click_position,
    cm.last_click_position,
    CASE WHEN s.result_count > 0 THEN COALESCE(cm.total_clicks, 0)::float / s.result_count ELSE 0 END AS click_through_rate,
    CASE WHEN s.result_count > 0 THEN 1 ELSE 0 END AS had_results,
    CASE WHEN COALESCE(cm.total_clicks, 0) > 0 THEN 1 ELSE 0 END AS was_clicked,
    CASE
        WHEN s.response_time_ms < 100 THEN 'fast'
        WHEN s.response_time_ms < 300 THEN 'medium'
        ELSE 'slow'
    END AS response_time_bucket
FROM search_base s
LEFT JOIN click_metrics cm ON s.search_query_id = cm.search_query_id
