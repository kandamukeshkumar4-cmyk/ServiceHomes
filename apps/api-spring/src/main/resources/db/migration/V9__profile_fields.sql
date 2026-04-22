ALTER TABLE profiles
    ADD COLUMN IF NOT EXISTS location VARCHAR(255);

CREATE TABLE IF NOT EXISTS profile_languages (
    profile_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    sort_order INT NOT NULL,
    language VARCHAR(64) NOT NULL,
    PRIMARY KEY (profile_id, sort_order)
);
