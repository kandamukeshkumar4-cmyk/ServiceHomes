CREATE INDEX IF NOT EXISTS idx_listing_availability_rules_listing_dates
    ON listing_availability_rules (listing_id, start_date, end_date);

CREATE INDEX IF NOT EXISTS idx_listing_availability_rules_listing_type_dates
    ON listing_availability_rules (listing_id, rule_type, start_date, end_date);
