{{
  config(
    materialized='table',
    schema='gold'
  )
}}

WITH search_clicks AS (
    SELECT
        listing_id,
        search_query_id,
        result_position,
        device_type,
        created_at AS clicked_at
    FROM {{ source('app', 'search_clicks') }}
),

search_queries AS (
    SELECT
        id AS search_query_id,
        query_hash,
        result_count,
        created_at AS searched_at
    FROM {{ source('app', 'search_queries') }}
),

listing_search_appearances AS (
    SELECT
        sc.listing_id,
        COUNT(DISTINCT sc.search_query_id) AS times_clicked,
        COUNT(DISTINCT sq.query_hash) AS unique_search_queries_clicked,
        AVG(sc.result_position) AS avg_click_position,
        MIN(sc.result_position) AS best_click_position
    FROM search_clicks sc
    JOIN search_queries sq ON sc.search_query_id = sq.search_query_id
    GROUP BY sc.listing_id
),

listing_search_impressions AS (
    SELECT
        sq.result_count AS total_results_in_searches,
        COUNT(DISTINCT sq.search_query_id) AS total_searches_with_results
    FROM search_queries sq
    WHERE sq.result_count > 0
),

reservations AS (
    SELECT
        listing_id,
        COUNT(*) FILTER (WHERE status = 'CONFIRMED') AS confirmed_bookings
    FROM {{ ref('sr_reservations') }}
    GROUP BY listing_id
)

SELECT
    lsa.listing_id,
    lsa.times_clicked,
    lsa.unique_search_queries_clicked,
    lsa.avg_click_position,
    lsa.best_click_position,
    COALESCE(r.confirmed_bookings, 0) AS confirmed_bookings,
    CASE WHEN lsa.times_clicked > 0 THEN
        COALESCE(r.confirmed_bookings, 0)::float / lsa.times_clicked
    ELSE 0 END AS booking_rate_from_clicks,
    lsa.times_clicked::float / NULLIF(lsi.total_searches_with_results, 0) AS relative_ctr
FROM listing_search_appearances lsa
CROSS JOIN listing_search_impressions lsi
LEFT JOIN reservations r ON lsa.listing_id = r.listing_id
