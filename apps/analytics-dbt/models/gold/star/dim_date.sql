{{
  config(
    materialized='table',
    schema='gold'
  )
}}

WITH date_spine AS (
  SELECT DATEADD(day, SEQ4(), '2020-01-01') as date_day
  FROM TABLE(GENERATOR(ROWCOUNT => 3650))
)

SELECT
  TO_CHAR(date_day, 'YYYYMMDD')::int as date_sk,
  date_day as full_date,
  YEAR(date_day) as year,
  QUARTER(date_day) as quarter,
  MONTH(date_day) as month,
  MONTHNAME(date_day) as month_name,
  DAYOFWEEK(date_day) as day_of_week,
  DAYNAME(date_day) as day_name,
  DAYOFMONTH(date_day) as day_of_month,
  DAYOFYEAR(date_day) as day_of_year,
  WEEKOFYEAR(date_day) as week_of_year
FROM date_spine
