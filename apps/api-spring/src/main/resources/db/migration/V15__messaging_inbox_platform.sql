DO $$
BEGIN
    IF to_regclass('public.threads') IS NULL AND to_regclass('public.message_threads') IS NOT NULL THEN
        ALTER TABLE message_threads RENAME TO threads;
    END IF;
END $$;

ALTER TABLE threads
    ADD COLUMN IF NOT EXISTS listing_id UUID REFERENCES listings(id),
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW();

UPDATE threads t
SET listing_id = r.listing_id
FROM reservations r
WHERE t.reservation_id = r.id
  AND t.listing_id IS NULL;

ALTER TABLE threads
    ALTER COLUMN listing_id SET NOT NULL;

ALTER TABLE messages
    ADD COLUMN IF NOT EXISTS attachment_url TEXT;

ALTER TABLE messages
    ALTER COLUMN content DROP NOT NULL;

ALTER TABLE messages
    DROP CONSTRAINT IF EXISTS chk_messages_content_or_attachment,
    ADD CONSTRAINT chk_messages_content_or_attachment CHECK (
        (content IS NOT NULL AND btrim(content) <> '')
        OR (attachment_url IS NOT NULL AND btrim(attachment_url) <> '')
    );

CREATE TABLE IF NOT EXISTS participant_read_receipts (
    thread_id UUID NOT NULL REFERENCES threads(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id),
    last_read_message_id UUID REFERENCES messages(id) ON DELETE SET NULL,
    read_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (thread_id, user_id)
);

INSERT INTO participant_read_receipts (thread_id, user_id, last_read_message_id, read_at)
SELECT
    t.id,
    participant.user_id,
    latest_message.id,
    COALESCE(latest_message.created_at, t.created_at)
FROM threads t
CROSS JOIN LATERAL (
    VALUES (t.guest_id), (t.host_id)
) participant(user_id)
LEFT JOIN LATERAL (
    SELECT m.id, m.created_at
    FROM messages m
    WHERE m.thread_id = t.id
      AND m.read_at IS NOT NULL
      AND m.sender_id <> participant.user_id
    ORDER BY m.created_at DESC
    LIMIT 1
) latest_message ON TRUE
ON CONFLICT (thread_id, user_id) DO NOTHING;

CREATE INDEX IF NOT EXISTS idx_messages_thread_created
    ON messages(thread_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_threads_participant
    ON threads(guest_id, host_id, updated_at DESC);

CREATE INDEX IF NOT EXISTS idx_threads_guest_updated
    ON threads(guest_id, updated_at DESC);

CREATE INDEX IF NOT EXISTS idx_threads_host_updated
    ON threads(host_id, updated_at DESC);

CREATE INDEX IF NOT EXISTS idx_participant_read
    ON participant_read_receipts(thread_id, user_id, read_at DESC);
