CREATE TABLE ticket (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    title           VARCHAR(200) NOT NULL,
    description     TEXT         NOT NULL,
    category        VARCHAR(20)  NOT NULL,
    priority        VARCHAR(20)  NOT NULL,
    status          VARCHAR(20)  NOT NULL,
    -- Temporary: replaced by the signed-in user's id once SSO is added.
    requester_email VARCHAR(320) NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL,
    updated_at      TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_ticket_status ON ticket (status);
CREATE INDEX idx_ticket_requester_email ON ticket (requester_email);
