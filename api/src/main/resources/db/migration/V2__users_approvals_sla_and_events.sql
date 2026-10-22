-- People who use the system. Identity (login, password, roles) lives in Keycloak;
-- this table is the directory the app needs: who manages whom, which support team
-- an agent belongs to. A row is created or linked the first time someone logs in.
CREATE TABLE app_user (
    id          UUID PRIMARY KEY,
    keycloak_id VARCHAR(64) UNIQUE,
    email       VARCHAR(320) NOT NULL UNIQUE,
    full_name   VARCHAR(200) NOT NULL,
    department  VARCHAR(100),
    job_title   VARCHAR(100),
    team        VARCHAR(20),
    manager_id  UUID REFERENCES app_user (id),
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL
);

-- Copy of the user's Keycloak roles, refreshed on every login. Needed to find
-- people by role when nobody is logged in (e.g. who to email for an approval).
CREATE TABLE app_user_role (
    user_id UUID        NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    role    VARCHAR(50) NOT NULL,
    PRIMARY KEY (user_id, role)
);

DROP INDEX idx_ticket_requester_email;
ALTER TABLE ticket DROP COLUMN requester_email;

ALTER TABLE ticket
    ADD COLUMN type                  VARCHAR(20)   NOT NULL DEFAULT 'INCIDENT',
    ADD COLUMN requester_id          UUID          NOT NULL REFERENCES app_user (id),
    ADD COLUMN assignee_id           UUID          REFERENCES app_user (id),
    ADD COLUMN purchase_amount       NUMERIC(12, 2),
    ADD COLUMN sla_target_seconds    BIGINT        NOT NULL,
    ADD COLUMN sla_due_at            TIMESTAMPTZ,
    ADD COLUMN sla_remaining_seconds BIGINT,
    ADD COLUMN sla_breached_at       TIMESTAMPTZ,
    ADD COLUMN first_response_at     TIMESTAMPTZ,
    ADD COLUMN resolved_at           TIMESTAMPTZ,
    ADD COLUMN version               BIGINT        NOT NULL DEFAULT 0;

ALTER TABLE ticket ALTER COLUMN type DROP DEFAULT;

CREATE INDEX idx_ticket_requester ON ticket (requester_id);
CREATE INDEX idx_ticket_assignee ON ticket (assignee_id);
CREATE INDEX idx_ticket_category_status ON ticket (category, status);
CREATE INDEX idx_ticket_sla_due ON ticket (sla_due_at) WHERE sla_breached_at IS NULL;

-- One row per approval a request must pass, in order.
-- An approver is either a specific person (their manager) or anyone with a role.
CREATE TABLE approval_step (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    ticket_id     BIGINT       NOT NULL REFERENCES ticket (id) ON DELETE CASCADE,
    step_order    INT          NOT NULL,
    name          VARCHAR(100) NOT NULL,
    approver_id   UUID         REFERENCES app_user (id),
    approver_role VARCHAR(50),
    status        VARCHAR(20)  NOT NULL,
    pending_since TIMESTAMPTZ,
    decided_by_id UUID         REFERENCES app_user (id),
    decided_at    TIMESTAMPTZ,
    comment       VARCHAR(1000),
    created_at    TIMESTAMPTZ  NOT NULL,
    UNIQUE (ticket_id, step_order),
    CHECK (approver_id IS NOT NULL OR approver_role IS NOT NULL)
);

CREATE INDEX idx_approval_step_pending ON approval_step (status, pending_since);

CREATE TABLE ticket_comment (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    ticket_id  BIGINT      NOT NULL REFERENCES ticket (id) ON DELETE CASCADE,
    author_id  UUID        NOT NULL REFERENCES app_user (id),
    body       TEXT        NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_ticket_comment_ticket ON ticket_comment (ticket_id);

-- Audit trail shown on the ticket page. actor_id is NULL for things the system did.
CREATE TABLE ticket_activity (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    ticket_id  BIGINT       NOT NULL REFERENCES ticket (id) ON DELETE CASCADE,
    actor_id   UUID         REFERENCES app_user (id),
    type       VARCHAR(40)  NOT NULL,
    message    VARCHAR(500) NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_ticket_activity_ticket ON ticket_activity (ticket_id);

-- Transactional outbox: events are saved in the same transaction as the change
-- that caused them, then a relay publishes them to Kafka. No event is lost if
-- Kafka is down, and none is sent for a change that was rolled back.
CREATE TABLE outbox_event (
    id           UUID PRIMARY KEY,
    aggregate_id VARCHAR(64) NOT NULL,
    type         VARCHAR(60) NOT NULL,
    payload      JSONB       NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL,
    published_at TIMESTAMPTZ
);

CREATE INDEX idx_outbox_event_unpublished ON outbox_event (created_at) WHERE published_at IS NULL;
