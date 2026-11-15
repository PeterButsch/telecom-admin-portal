-- Staff sign-in, roles and the audit trail.

CREATE TABLE staff_users (
    id                  INTEGER PRIMARY KEY AUTOINCREMENT,
    username            TEXT    NOT NULL UNIQUE COLLATE NOCASE CHECK (length(username) BETWEEN 3 AND 40),
    display_name        TEXT    NOT NULL,
    email               TEXT    NOT NULL UNIQUE COLLATE NOCASE,
    role                TEXT    NOT NULL CHECK (role IN ('CARE_AGENT', 'CARE_TEAM_LEAD', 'FRAUD_ANALYST',
                                                         'NETWORK_OPS', 'PRODUCT_MANAGER', 'ADMIN')),
    password_hash       TEXT    NOT NULL,
    is_active           INTEGER NOT NULL DEFAULT 1 CHECK (is_active IN (0, 1)),
    failed_logins       INTEGER NOT NULL DEFAULT 0 CHECK (failed_logins >= 0),
    locked_until        TEXT,
    password_changed_at TEXT    NOT NULL,
    last_login_at       TEXT,
    created_at          TEXT    NOT NULL
);

-- Actor columns are copied rather than referenced so the history survives changes to staff accounts.
CREATE TABLE audit_events (
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    occurred_at    TEXT NOT NULL,
    actor_id       INTEGER,
    actor_username TEXT NOT NULL,
    actor_role     TEXT,
    action         TEXT NOT NULL,
    entity_type    TEXT,
    entity_id      TEXT,
    summary        TEXT NOT NULL,
    outcome        TEXT NOT NULL CHECK (outcome IN ('SUCCESS', 'FAILED', 'DENIED'))
);
CREATE INDEX ix_audit_occurred ON audit_events (occurred_at);
CREATE INDEX ix_audit_entity ON audit_events (entity_type, entity_id);
CREATE INDEX ix_audit_actor ON audit_events (actor_username);

CREATE TRIGGER audit_events_no_update BEFORE UPDATE ON audit_events
BEGIN
    SELECT RAISE(ABORT, 'audit events cannot be changed');
END;

CREATE TRIGGER audit_events_no_delete BEFORE DELETE ON audit_events
BEGIN
    SELECT RAISE(ABORT, 'audit events cannot be deleted');
END;
