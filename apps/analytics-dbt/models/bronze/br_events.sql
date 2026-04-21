{{
  config(
    materialized='table',
    schema='bronze'
  )
}}

SELECT
  value:id::string as event_id,
  value:eventType::string as event_type,
  value:eventVersion::string as event_version,
  value:aggregateType::string as aggregate_type,
  value:aggregateId::string as aggregate_id,
  value:payload as payload,
  value:metadata as metadata,
  value:createdAt::timestamp as created_at,
  metadata$filename as source_file,
  metadata$file_row_number as source_row_number
FROM @raw.events_stage
  (file_format => raw.jsonl_format)
