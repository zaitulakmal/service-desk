-- Demo company directory and sample tickets (the same ones as the UI mockup).
-- Loaded only when FLYWAY_LOCATIONS includes classpath:db/demo (the default for local
-- development and the demo deployment). Safe to re-run.
-- Logins are in Keycloak (deploy/keycloak/servicedesk-realm.json); people are matched by email.

INSERT INTO app_user (id, email, full_name, department, job_title, team, created_at, updated_at) VALUES
    ('00000000-0000-4000-8000-000000000002', 'farah@example.com',  'Farah Aziz',   'Finance', 'Finance Manager',      NULL, now(), now()),
    ('00000000-0000-4000-8000-000000000004', 'kumar@example.com',  'Kumar Raj',    'Sales',   'Sales Manager',        NULL, now(), now()),
    ('00000000-0000-4000-8000-000000000007', 'siti@example.com',   'Siti Hajar',   'IT',      'IT Service Manager',   NULL, now(), now()),
    ('00000000-0000-4000-8000-000000000001', 'aina@example.com',   'Aina Rahman',  'Finance', 'Finance Executive',    NULL, now(), now()),
    ('00000000-0000-4000-8000-000000000003', 'hafiz@example.com',  'Hafiz Osman',  'Sales',   'Account Executive',    NULL, now(), now()),
    ('00000000-0000-4000-8000-000000000005', 'daniel@example.com', 'Daniel Lee',   'IT',      'IT Support Engineer',  'IT', now(), now()),
    ('00000000-0000-4000-8000-000000000006', 'mei@example.com',    'Mei Ling Tan', 'HR',      'HR Executive',         'HR', now(), now())
ON CONFLICT (email) DO UPDATE
    SET department = EXCLUDED.department,
        job_title  = EXCLUDED.job_title,
        team       = EXCLUDED.team;

UPDATE app_user SET manager_id = (SELECT id FROM app_user WHERE email = 'farah@example.com')
WHERE email = 'aina@example.com';
UPDATE app_user SET manager_id = (SELECT id FROM app_user WHERE email = 'kumar@example.com')
WHERE email = 'hafiz@example.com';
UPDATE app_user SET manager_id = (SELECT id FROM app_user WHERE email = 'siti@example.com')
WHERE email IN ('daniel@example.com');

-- Same roles as Keycloak, so approvals and escalations can be routed before anyone logs in.
INSERT INTO app_user_role (user_id, role)
SELECT u.id, r.role
FROM app_user u
JOIN (VALUES
    ('aina@example.com', 'staff'), ('farah@example.com', 'staff'), ('hafiz@example.com', 'staff'),
    ('kumar@example.com', 'staff'), ('daniel@example.com', 'staff'), ('daniel@example.com', 'agent'),
    ('daniel@example.com', 'security-approver'), ('mei@example.com', 'staff'), ('mei@example.com', 'agent'),
    ('siti@example.com', 'staff'), ('siti@example.com', 'service-manager')
) AS r (email, role) ON r.email = u.email
ON CONFLICT DO NOTHING;

DO $$
DECLARE
    aina   uuid := (SELECT id FROM app_user WHERE email = 'aina@example.com');
    farah  uuid := (SELECT id FROM app_user WHERE email = 'farah@example.com');
    hafiz  uuid := (SELECT id FROM app_user WHERE email = 'hafiz@example.com');
    kumar  uuid := (SELECT id FROM app_user WHERE email = 'kumar@example.com');
    daniel uuid := (SELECT id FROM app_user WHERE email = 'daniel@example.com');
    mei    uuid := (SELECT id FROM app_user WHERE email = 'mei@example.com');
    t      timestamptz := now();
BEGIN
    IF EXISTS (SELECT 1 FROM ticket WHERE id = 1042) THEN
        RETURN;
    END IF;

    INSERT INTO ticket (id, title, description, type, category, priority, status, requester_id, assignee_id,
                        purchase_amount, sla_target_seconds, sla_due_at, sla_remaining_seconds, sla_breached_at,
                        first_response_at, resolved_at, created_at, updated_at)
    OVERRIDING SYSTEM VALUE VALUES
    (1019, 'Replace broken desk chair (Level 12)', 'The gas lift on my chair is broken and it sinks all day.',
        'SERVICE_REQUEST', 'FACILITIES', 'LOW', 'CLOSED', aina, NULL, NULL,
        432000, t - interval '11 days', NULL, NULL, t - interval '15 days', t - interval '14 days',
        t - interval '16 days', t - interval '14 days'),
    (1027, 'Update bank account for payroll', 'I changed banks. Please update my salary account before the next payroll run.',
        'SERVICE_REQUEST', 'HR', 'MEDIUM', 'RESOLVED', aina, mei, NULL,
        259200, t - interval '3 days', NULL, NULL, t - interval '5 days 20 hours', t - interval '5 days',
        t - interval '6 days', t - interval '5 days'),
    (1031, 'Claim for client dinner (RM 420)', 'Dinner with a client team on 14 October. Receipt attached.',
        'SERVICE_REQUEST', 'FINANCE', 'MEDIUM', 'OPEN', aina, NULL, NULL,
        259200, t + interval '1 day', NULL, NULL, NULL, NULL,
        t - interval '2 days', t - interval '2 days'),
    (1034, 'Adobe Acrobat Pro licence', 'I need Acrobat Pro to edit and sign customer contracts.',
        'PURCHASE', 'IT', 'MEDIUM', 'AWAITING_APPROVAL', hafiz, NULL, 1450.00,
        259200, NULL, 259200, NULL, NULL, NULL,
        t - interval '2 days 4 hours', t - interval '2 days 4 hours'),
    (1036, 'Printer on Level 12 jams every page', 'The printer near the pantry jams on every page since this morning.',
        'INCIDENT', 'IT', 'LOW', 'OPEN', kumar, NULL, NULL,
        432000, t + interval '4 days 22 hours', NULL, NULL, NULL, NULL,
        t - interval '2 hours', t - interval '2 hours'),
    (1039, 'Access to SAP Finance module', 'I need access to the SAP Finance module to pull the GL reports for month-end close. Read-only is fine.',
        'ACCESS', 'IT', 'MEDIUM', 'AWAITING_APPROVAL', aina, daniel, NULL,
        259200, NULL, 259200, NULL, t - interval '1 day 5 hours', NULL,
        t - interval '1 day 6 hours', t - interval '1 day 3 hours'),
    (1041, 'Laptop setup for a new joiner in Finance', 'Nurul joins Finance next month. Please prepare a laptop with the standard Finance software.',
        'SERVICE_REQUEST', 'IT', 'MEDIUM', 'IN_PROGRESS', farah, daniel, NULL,
        259200, t + interval '1 day 3 hours', NULL, NULL, t - interval '1 day 20 hours', NULL,
        t - interval '1 day 21 hours', t - interval '1 day 20 hours'),
    (1042, 'Laptop screen flickering when docked', 'Since Monday my screen flickers every few seconds when the laptop is connected to the dock on Level 12. It works fine without the dock.',
        'INCIDENT', 'IT', 'HIGH', 'IN_PROGRESS', aina, daniel, NULL,
        28800, t + interval '2 hours 15 minutes', NULL, NULL, t - interval '5 hours', NULL,
        t - interval '5 hours 45 minutes', t - interval '2 hours'),
    (1043, 'Second monitor for home office', 'A 27-inch monitor for working from home two days a week.',
        'PURCHASE', 'IT', 'LOW', 'AWAITING_APPROVAL', aina, NULL, 1200.00,
        432000, NULL, 432000, NULL, NULL, NULL,
        t - interval '6 hours', t - interval '6 hours'),
    (1044, 'Outlook keeps asking for my password', 'Outlook asks me to sign in again every few minutes since this morning.',
        'INCIDENT', 'IT', 'HIGH', 'OPEN', mei, NULL, NULL,
        28800, t + interval '40 minutes', NULL, NULL, NULL, NULL,
        t - interval '7 hours 20 minutes', t - interval '7 hours 20 minutes'),
    (1045, 'VPN not connecting from home', 'The VPN client times out at "Connecting". I cannot reach any internal system and have a customer demo at 3pm.',
        'INCIDENT', 'IT', 'URGENT', 'OPEN', hafiz, NULL, NULL,
        14400, t - interval '25 minutes', NULL, t - interval '24 minutes', NULL, NULL,
        t - interval '4 hours 25 minutes', t - interval '24 minutes');

    PERFORM setval(pg_get_serial_sequence('ticket', 'id'), (SELECT max(id) FROM ticket));

    INSERT INTO approval_step (ticket_id, step_order, name, approver_id, approver_role, status, pending_since,
                               decided_by_id, decided_at, comment, created_at) VALUES
    (1034, 1, 'Manager approval', kumar, NULL, 'PENDING', t - interval '2 days 4 hours', NULL, NULL, NULL, t - interval '2 days 4 hours'),
    (1039, 1, 'Manager approval', farah, NULL, 'APPROVED', NULL, farah, t - interval '1 day 3 hours',
        'Needed for month-end close. Read-only is enough.', t - interval '1 day 6 hours'),
    (1039, 2, 'IT Security approval', NULL, 'security-approver', 'PENDING', t - interval '1 day 3 hours', NULL, NULL, NULL, t - interval '1 day 6 hours'),
    (1043, 1, 'Manager approval', farah, NULL, 'PENDING', t - interval '6 hours', NULL, NULL, NULL, t - interval '6 hours');

    INSERT INTO ticket_comment (ticket_id, author_id, body, created_at) VALUES
    (1039, farah, 'Approved from my side. Please keep it read-only.', t - interval '1 day 3 hours'),
    (1042, daniel, 'Thanks Aina. Could you try a different USB-C cable? If it still flickers I will swap the dock this afternoon.', t - interval '2 hours');

    INSERT INTO ticket_activity (ticket_id, actor_id, type, message, created_at) VALUES
    (1019, aina,   'CREATED',            'Aina Rahman created the request', t - interval '16 days'),
    (1019, NULL,   'STATUS_CHANGED',     'Facilities replaced the chair. Request closed.', t - interval '14 days'),
    (1027, aina,   'CREATED',            'Aina Rahman created the request', t - interval '6 days'),
    (1027, mei,    'ASSIGNED',           'Mei Ling Tan took the request', t - interval '5 days 20 hours'),
    (1027, mei,    'STATUS_CHANGED',     'Mei Ling Tan changed the status: In progress → Resolved', t - interval '5 days'),
    (1031, aina,   'CREATED',            'Aina Rahman created the request', t - interval '2 days'),
    (1034, hafiz,  'CREATED',            'Hafiz Osman created the request', t - interval '2 days 4 hours'),
    (1034, NULL,   'APPROVAL_REQUESTED', 'Waiting for Kumar Raj to approve. SLA paused.', t - interval '2 days 4 hours'),
    (1036, kumar,  'CREATED',            'Kumar Raj created the request', t - interval '2 hours'),
    (1039, aina,   'CREATED',            'Aina Rahman created the request', t - interval '1 day 6 hours'),
    (1039, NULL,   'APPROVAL_REQUESTED', 'Waiting for Farah Aziz to approve. SLA paused.', t - interval '1 day 6 hours'),
    (1039, daniel, 'ASSIGNED',           'Daniel Lee took the request', t - interval '1 day 5 hours'),
    (1039, farah,  'APPROVED',           'Farah Aziz approved (Manager approval)', t - interval '1 day 3 hours'),
    (1039, NULL,   'APPROVAL_REQUESTED', 'Waiting for IT Security to approve. SLA paused.', t - interval '1 day 3 hours'),
    (1039, farah,  'COMMENTED',          'Farah Aziz replied', t - interval '1 day 3 hours'),
    (1041, farah,  'CREATED',            'Farah Aziz created the request', t - interval '1 day 21 hours'),
    (1041, daniel, 'ASSIGNED',           'Daniel Lee took the request', t - interval '1 day 20 hours'),
    (1042, aina,   'CREATED',            'Aina Rahman created the request', t - interval '5 hours 45 minutes'),
    (1042, daniel, 'ASSIGNED',           'Daniel Lee took the request', t - interval '5 hours'),
    (1042, daniel, 'COMMENTED',          'Daniel Lee replied', t - interval '2 hours'),
    (1043, aina,   'CREATED',            'Aina Rahman created the request', t - interval '6 hours'),
    (1043, NULL,   'APPROVAL_REQUESTED', 'Waiting for Farah Aziz to approve. SLA paused.', t - interval '6 hours'),
    (1044, mei,    'CREATED',            'Mei Ling Tan created the request', t - interval '7 hours 20 minutes'),
    (1045, hafiz,  'CREATED',            'Hafiz Osman created the request', t - interval '4 hours 25 minutes'),
    (1045, NULL,   'SLA_BREACHED',       'SLA breached. Escalated to service management.', t - interval '24 minutes');
END $$;
