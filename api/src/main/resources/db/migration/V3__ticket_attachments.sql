-- The file itself lives in S3 (storage_key); this row is what the app knows about it.
CREATE TABLE ticket_attachment (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    ticket_id      BIGINT       NOT NULL REFERENCES ticket (id) ON DELETE CASCADE,
    file_name      VARCHAR(255) NOT NULL,
    content_type   VARCHAR(100) NOT NULL,
    size_bytes     BIGINT       NOT NULL,
    storage_key    VARCHAR(300) NOT NULL UNIQUE,
    uploaded_by_id UUID         NOT NULL REFERENCES app_user (id),
    created_at     TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_ticket_attachment_ticket ON ticket_attachment (ticket_id);
