CREATE TABLE IF NOT EXISTS listing_categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(64) NOT NULL UNIQUE,
    icon VARCHAR(64),
    description VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS listing_amenities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(64) NOT NULL UNIQUE,
    icon VARCHAR(64),
    category VARCHAR(64)
);

CREATE TABLE IF NOT EXISTS listings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    host_id UUID NOT NULL REFERENCES users(id),
    title VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    category_id UUID NOT NULL REFERENCES listing_categories(id),
    property_type VARCHAR(32) NOT NULL,
    max_guests INT NOT NULL,
    bedrooms INT NOT NULL,
    beds INT NOT NULL,
    bathrooms INT NOT NULL,
    nightly_price NUMERIC(10, 2) NOT NULL,
    cleaning_fee NUMERIC(10, 2),
    service_fee NUMERIC(10, 2),
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    published_at TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS listing_locations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    listing_id UUID NOT NULL UNIQUE REFERENCES listings(id) ON DELETE CASCADE,
    address_line1 VARCHAR(255) NOT NULL,
    address_line2 VARCHAR(255),
    city VARCHAR(128) NOT NULL,
    state VARCHAR(128),
    postal_code VARCHAR(32),
    country VARCHAR(128) NOT NULL,
    latitude NUMERIC(10, 8),
    longitude NUMERIC(11, 8)
);

CREATE TABLE IF NOT EXISTS listing_policies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    listing_id UUID NOT NULL UNIQUE REFERENCES listings(id) ON DELETE CASCADE,
    check_in_time TIME,
    check_out_time TIME,
    min_nights INT NOT NULL DEFAULT 1,
    max_nights INT,
    cancellation_policy VARCHAR(32) NOT NULL DEFAULT 'FLEXIBLE',
    instant_book BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS listing_photos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    listing_id UUID NOT NULL REFERENCES listings(id) ON DELETE CASCADE,
    s3_key VARCHAR(500) NOT NULL,
    url VARCHAR(500) NOT NULL,
    order_index INT NOT NULL DEFAULT 0,
    is_cover BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS listing_amenity_links (
    listing_id UUID NOT NULL REFERENCES listings(id) ON DELETE CASCADE,
    amenity_id UUID NOT NULL REFERENCES listing_amenities(id) ON DELETE CASCADE,
    PRIMARY KEY (listing_id, amenity_id)
);

CREATE TABLE IF NOT EXISTS listing_availability_rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    listing_id UUID NOT NULL REFERENCES listings(id) ON DELETE CASCADE,
    rule_type VARCHAR(32) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    value NUMERIC(10, 2),
    CONSTRAINT chk_dates CHECK (start_date <= end_date)
);

-- Seed categories
INSERT INTO listing_categories (name, icon, description) VALUES
('Trending', 'pi pi-chart-line', 'Most popular right now'),
('Beachfront', 'pi pi-sun', 'Right on the beach'),
('Cabins', 'pi pi-home', 'Rustic cabin stays'),
('Tiny homes', 'pi pi-box', 'Compact and cozy'),
('Amazing pools', 'pi pi-swimming', 'Poolside paradise'),
('Farms', 'pi pi-globe', 'Rural escapes'),
('Treehouses', 'pi pi-sitemap', 'Elevated stays'),
('Camping', 'pi pi-flag', 'Outdoor adventures'),
('Castles', 'pi pi-building', 'Royal experiences'),
('Boats', 'pi pi-send', 'Stay on the water'),
('Arctic', 'pi pi-snowflake', 'Cold climate stays'),
('Desert', 'pi pi-cloud', 'Hot and dry escapes')
ON CONFLICT (name) DO NOTHING;

-- Seed amenities
INSERT INTO listing_amenities (name, icon, category) VALUES
('WiFi', 'pi pi-wifi', 'Essentials'),
('Kitchen', 'pi pi-utensils', 'Essentials'),
('Washer', 'pi pi-sync', 'Essentials'),
('Dryer', 'pi pi-sync', 'Essentials'),
('Air conditioning', 'pi pi-sun', 'Climate'),
('Heating', 'pi pi-sun', 'Climate'),
('Pool', 'pi pi-swimming', 'Features'),
('Hot tub', 'pi pi-heart', 'Features'),
('Free parking', 'pi pi-car', 'Parking'),
('EV charger', 'pi pi-bolt', 'Parking'),
('Workspace', 'pi pi-desktop', 'Work'),
('TV', 'pi pi-video', 'Entertainment'),
('Fireplace', 'pi pi-fire', 'Features'),
('BBQ grill', 'pi pi-users', 'Outdoor'),
('Patio', 'pi pi-home', 'Outdoor'),
('Garden', 'pi pi-globe', 'Outdoor')
ON CONFLICT (name) DO NOTHING;
