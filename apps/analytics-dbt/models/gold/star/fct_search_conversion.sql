{{
  config(
    materialized='table',
    schema='gold'
  )
}}

WITH search_sessions AS (
    SELECT * FROM {{ ref('fct_search_session') }}
),

reservations AS (
    SELECT
        id AS reservation_id,
        guest_id,
        listing_id,
        status,
        check_in,
        check_out,
        total_amount,
        created_at AS reserved_at
    FROM {{ ref('sr_reservations') }}
),

search_to_click AS (
    SELECT
        ss.search_query_id,
        ss.user_id,
        ss.query_hash,
        ss.searched_at,
        ss.result_count,
        ss.total_clicks,
        ss.click_through_rate,
        ss.had_results,
        ss.was_clicked,
        CASE WHEN ss.was_clicked = 1 THEN 1 ELSE 0 END AS reached_click_stage
    FROM search_sessions ss
),

click_to_booking AS (
    SELECT
        stc.*,
        COUNT(DISTINCT r.reservation_id) AS bookings_from_search,
        COUNT(DISTINCT CASE WHEN r.status = 'CONFIRMED' THEN r.reservation_id END) AS confirmed_bookings_from_search,
        SUM(COALESCE(r.total_amount, 0)) AS total_booking_value
    FROM search_to_click stc
    LEFT JOIN reservations r
        ON stc.user_id = r.guest_id
        AND r.reserved_at >= stc.searched_at
        AND r.reserved_at < stc.searched_at + INTERVAL '24 hours'
    GROUP BY
        stc.search_query_id,
        stc.user_id,
        stc.query_hash,
        stc.searched_at,
        stc.result_count,
        stc.total_clicks,
        stc.click_through_rate,
        stc.had_results,
        stc.was_clicked,
        stc.reached_click_stage
)

SELECT
    search_query_id,
    user_id,
    query_hash,
    searched_at,
    result_count,
    total_clicks,
    click_through_rate,
    had_results,
    was_clicked,
    reached_click_stage,
    bookings_from_search,
    confirmed_bookings_from_search,
    total_booking_value,
    CASE WHEN bookings_from_search > 0 THEN 1 ELSE 0 END AS converted_to_booking,
    CASE WHEN had_results = 1 AND was_clicked = 1 THEN
        bookings_from_search::float / NULLIF(total_clicks, 0)
    ELSE NULL END AS click_to_booking_rate,
    CASE WHEN had_results = 1 THEN
        bookings_from_search::float / NULLIF(result_count, 0)
    ELSE NULL END AS search_to_booking_rate
FROM click_to_booking
