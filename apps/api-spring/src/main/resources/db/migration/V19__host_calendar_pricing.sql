CREATE TABLE seasonal_pricing_templates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    listing_id UUID NOT NULL REFERENCES listings(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    multiplier NUMERIC(5, 2) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT seasonal_pricing_template_dates_check CHECK (end_date >= start_date),
    CONSTRAINT seasonal_pricing_template_multiplier_positive CHECK (multiplier > 0)
);

CREATE INDEX idx_seasonal_pricing_templates_listing_id ON seasonal_pricing_templates(listing_id);
CREATE INDEX idx_seasonal_pricing_templates_date_range ON seasonal_pricing_templates(listing_id, start_date, end_date);

CREATE TABLE length_of_stay_discounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    listing_id UUID NOT NULL REFERENCES listings(id) ON DELETE CASCADE,
    min_nights INT NOT NULL,
    discount_percent NUMERIC(5, 2) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT length_of_stay_discount_min_nights_positive CHECK (min_nights > 0),
    CONSTRAINT length_of_stay_discount_percent_range CHECK (discount_percent >= 0 AND discount_percent <= 100)
);

CREATE INDEX idx_length_of_stay_discounts_listing_id ON length_of_stay_discounts(listing_id);
CREATE INDEX idx_length_of_stay_discounts_min_nights ON length_of_stay_discounts(listing_id, min_nights);

CREATE TABLE weekend_multipliers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    listing_id UUID NOT NULL REFERENCES listings(id) ON DELETE CASCADE,
    friday_multiplier NUMERIC(5, 2) NOT NULL DEFAULT 1.00,
    saturday_multiplier NUMERIC(5, 2) NOT NULL DEFAULT 1.00,
    sunday_multiplier NUMERIC(5, 2) NOT NULL DEFAULT 1.00,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT weekend_multipliers_positive CHECK (friday_multiplier > 0 AND saturday_multiplier > 0 AND sunday_multiplier > 0)
);

CREATE UNIQUE INDEX idx_weekend_multipliers_listing_id ON weekend_multipliers(listing_id);

CREATE TABLE turnover_days (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    listing_id UUID NOT NULL REFERENCES listings(id) ON DELETE CASCADE,
    buffer_days INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT turnover_days_buffer_non_negative CHECK (buffer_days >= 0)
);

CREATE UNIQUE INDEX idx_turnover_days_listing_id ON turnover_days(listing_id);
