-- Kafka may deliver the same event more than once. Remembering which events were
-- handled lets the service ignore repeats instead of emailing people twice.
CREATE TABLE processed_event (
    event_id     UUID PRIMARY KEY,
    type         VARCHAR(60) NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL
);

-- What was sent to whom, for support questions like "did I get an email about this?".
CREATE TABLE notification (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    event_id   UUID         NOT NULL REFERENCES processed_event (event_id),
    ticket_id  BIGINT       NOT NULL,
    recipient  VARCHAR(320) NOT NULL,
    subject    VARCHAR(300) NOT NULL,
    sent_at    TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_notification_ticket ON notification (ticket_id);
