CREATE TABLE IF NOT EXISTS notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    type TEXT NOT NULL CHECK (type IN (
        'RESERVATION_CREATED',
        'RESERVATION_CONFIRMED',
        'MESSAGE_RECEIVED',
        'REVIEW_RECEIVED',
        'PAYOUT_SCHEDULED',
        'LISTING_PUBLISHED',
        'SYSTEM'
    )),
    title TEXT NOT NULL,
    body TEXT NOT NULL,
    data_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    read_at TIMESTAMPTZ,
    dismissed_at TIMESTAMPTZ,
    channel TEXT NOT NULL CHECK (channel IN ('IN_APP', 'EMAIL', 'PUSH')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS notification_preferences (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    channel TEXT NOT NULL CHECK (channel IN ('IN_APP', 'EMAIL', 'PUSH')),
    notification_type TEXT NOT NULL CHECK (notification_type IN (
        'RESERVATION_CREATED',
        'RESERVATION_CONFIRMED',
        'MESSAGE_RECEIVED',
        'REVIEW_RECEIVED',
        'PAYOUT_SCHEDULED',
        'LISTING_PUBLISHED',
        'SYSTEM'
    )),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, channel, notification_type)
);

CREATE INDEX IF NOT EXISTS idx_notifications_user_unread
    ON notifications(user_id, read_at)
    WHERE dismissed_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_notifications_user_created
    ON notifications(user_id, created_at DESC)
    WHERE dismissed_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_notifications_user_type_channel_created
    ON notifications(user_id, type, channel, created_at DESC);
