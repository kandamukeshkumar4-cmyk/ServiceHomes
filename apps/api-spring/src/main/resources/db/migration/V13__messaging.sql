CREATE TABLE IF NOT EXISTS message_threads (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reservation_id UUID NOT NULL UNIQUE REFERENCES reservations(id) ON DELETE CASCADE,
    guest_id UUID NOT NULL REFERENCES users(id),
    host_id UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    thread_id UUID NOT NULL REFERENCES message_threads(id) ON DELETE CASCADE,
    sender_id UUID NOT NULL REFERENCES users(id),
    content TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    read_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_message_threads_guest_created
    ON message_threads(guest_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_message_threads_host_created
    ON message_threads(host_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_messages_thread_created
    ON messages(thread_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_messages_thread_unread
    ON messages(thread_id, read_at, created_at DESC);
