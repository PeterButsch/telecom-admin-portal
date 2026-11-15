-- Singtel consumer and enterprise mobile model.
-- Timestamps are UTC text "yyyy-MM-dd HH:mm:ss.SSS" so they sort and compare correctly as strings.
-- Dates are "yyyy-MM-dd". Booleans are 0/1. Money is SGD.

CREATE TABLE plans (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    code            TEXT    NOT NULL UNIQUE,
    name            TEXT    NOT NULL,
    brand           TEXT    NOT NULL CHECK (brand IN ('SINGTEL', 'GOMO', 'HI_CARD')),
    category        TEXT    NOT NULL CHECK (category IN ('POSTPAID', 'SIM_ONLY', 'PREPAID', 'DATA_ONLY')),
    segment         TEXT    NOT NULL CHECK (segment IN ('CONSUMER', 'ENTERPRISE')),
    monthly_price   NUMERIC NOT NULL CHECK (monthly_price >= 0),
    data_limit_gb   INTEGER CHECK (data_limit_gb >= 0),
    minutes_limit   INTEGER CHECK (minutes_limit >= 0),
    sms_limit       INTEGER CHECK (sms_limit >= 0),
    contract_months INTEGER NOT NULL DEFAULT 0 CHECK (contract_months IN (0, 12, 24)),
    network         TEXT    NOT NULL DEFAULT '5G' CHECK (network IN ('4G', '5G')),
    is_active       INTEGER NOT NULL DEFAULT 1 CHECK (is_active IN (0, 1)),
    created_at      TEXT    NOT NULL
);

CREATE TABLE add_ons (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    code          TEXT    NOT NULL UNIQUE,
    name          TEXT    NOT NULL,
    category      TEXT    NOT NULL CHECK (category IN ('DATA', 'ROAMING', 'VALUE_ADDED', 'INSURANCE')),
    monthly_price NUMERIC NOT NULL CHECK (monthly_price >= 0),
    is_active     INTEGER NOT NULL DEFAULT 1 CHECK (is_active IN (0, 1))
);

CREATE TABLE promotions (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    code            TEXT    NOT NULL UNIQUE,
    name            TEXT    NOT NULL,
    discount_type   TEXT    NOT NULL CHECK (discount_type IN ('PERCENT', 'AMOUNT')),
    discount_value  NUMERIC NOT NULL CHECK (discount_value > 0),
    duration_months INTEGER NOT NULL CHECK (duration_months > 0),
    valid_from      TEXT    NOT NULL,
    valid_to        TEXT,
    is_active       INTEGER NOT NULL DEFAULT 1 CHECK (is_active IN (0, 1))
);

CREATE TABLE customers (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    customer_number TEXT    NOT NULL UNIQUE,
    type            TEXT    NOT NULL CHECK (type IN ('INDIVIDUAL', 'BUSINESS')),
    first_name      TEXT,
    last_name       TEXT,
    company_name    TEXT,
    uen             TEXT UNIQUE,
    contact_name    TEXT,
    id_type         TEXT CHECK (id_type IN ('NRIC', 'FIN', 'PASSPORT')),
    id_number       TEXT,
    date_of_birth   TEXT,
    email           TEXT    NOT NULL UNIQUE COLLATE NOCASE,
    phone           TEXT    NOT NULL,
    address_line    TEXT,
    postal_code     TEXT CHECK (postal_code IS NULL OR (length(postal_code) = 6 AND postal_code NOT GLOB '*[^0-9]*')),
    status          TEXT    NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'SUSPENDED', 'CLOSED')),
    created_at      TEXT    NOT NULL,
    CHECK (
        (type = 'INDIVIDUAL' AND first_name IS NOT NULL AND last_name IS NOT NULL
            AND id_type IS NOT NULL AND id_number IS NOT NULL)
        OR (type = 'BUSINESS' AND company_name IS NOT NULL AND uen IS NOT NULL)
    )
);
CREATE UNIQUE INDEX ux_customers_identity ON customers (id_type, id_number) WHERE id_number IS NOT NULL;

CREATE TABLE accounts (
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    account_number TEXT    NOT NULL UNIQUE,
    customer_id    INTEGER NOT NULL REFERENCES customers (id),
    type           TEXT    NOT NULL CHECK (type IN ('POSTPAID', 'PREPAID')),
    name           TEXT,
    bill_cycle_day INTEGER CHECK (bill_cycle_day BETWEEN 1 AND 28),
    payment_method TEXT    NOT NULL CHECK (payment_method IN ('GIRO', 'CREDIT_CARD', 'PAYNOW', 'INVOICE', 'TOP_UP')),
    balance        NUMERIC NOT NULL DEFAULT 0,
    credit_limit   NUMERIC CHECK (credit_limit >= 0),
    status         TEXT    NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'SUSPENDED', 'CLOSED')),
    created_at     TEXT    NOT NULL
);
CREATE INDEX ix_accounts_customer ON accounts (customer_id);

CREATE TABLE mobile_lines (
    id                  INTEGER PRIMARY KEY AUTOINCREMENT,
    account_id          INTEGER NOT NULL REFERENCES accounts (id),
    plan_id             INTEGER NOT NULL REFERENCES plans (id),
    msisdn              TEXT    NOT NULL UNIQUE
        CHECK (length(msisdn) = 8 AND substr(msisdn, 1, 1) IN ('8', '9') AND msisdn NOT GLOB '*[^0-9]*'),
    sim_type            TEXT    NOT NULL CHECK (sim_type IN ('PHYSICAL', 'ESIM')),
    iccid               TEXT    NOT NULL UNIQUE,
    status              TEXT    NOT NULL CHECK (status IN ('PENDING', 'ACTIVE', 'SUSPENDED', 'BARRED', 'TERMINATED')),
    status_reason       TEXT,
    user_name           TEXT,
    promotion_id        INTEGER REFERENCES promotions (id),
    promotion_ends_on   TEXT,
    contract_start      TEXT,
    contract_end        TEXT,
    port_in_from        TEXT CHECK (port_in_from IN ('M1', 'STARHUB', 'SIMBA', 'CIRCLES_LIFE', 'OTHER')),
    port_out_to         TEXT CHECK (port_out_to IN ('M1', 'STARHUB', 'SIMBA', 'CIRCLES_LIFE', 'OTHER')),
    activated_at        TEXT,
    terminated_at       TEXT,
    created_at          TEXT    NOT NULL,
    CHECK ((status = 'TERMINATED') = (terminated_at IS NOT NULL)),
    CHECK (port_out_to IS NULL OR status = 'TERMINATED')
);
CREATE INDEX ix_lines_account ON mobile_lines (account_id);
CREATE INDEX ix_lines_plan ON mobile_lines (plan_id);
CREATE INDEX ix_lines_status ON mobile_lines (status);

CREATE TABLE line_add_ons (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    line_id    INTEGER NOT NULL REFERENCES mobile_lines (id),
    add_on_id  INTEGER NOT NULL REFERENCES add_ons (id),
    started_at TEXT    NOT NULL,
    ended_at   TEXT
);
CREATE INDEX ix_line_add_ons_line ON line_add_ons (line_id);

CREATE TABLE devices (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    imei         TEXT NOT NULL UNIQUE CHECK (length(imei) BETWEEN 14 AND 16 AND imei NOT GLOB '*[^0-9]*'),
    manufacturer TEXT NOT NULL,
    model        TEXT NOT NULL,
    status       TEXT NOT NULL DEFAULT 'AVAILABLE' CHECK (status IN ('AVAILABLE', 'ASSIGNED', 'LOST', 'DAMAGED')),
    line_id      INTEGER REFERENCES mobile_lines (id),
    assigned_at  TEXT,
    created_at   TEXT NOT NULL,
    CHECK ((status = 'ASSIGNED') = (line_id IS NOT NULL))
);
CREATE INDEX ix_devices_line ON devices (line_id);

CREATE TABLE usage_records (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    line_id     INTEGER NOT NULL REFERENCES mobile_lines (id),
    type        TEXT    NOT NULL CHECK (type IN ('CALL', 'DATA', 'SMS')),
    quantity    NUMERIC NOT NULL CHECK (quantity >= 0),
    cost        NUMERIC NOT NULL CHECK (cost >= 0),
    roaming     INTEGER NOT NULL DEFAULT 0 CHECK (roaming IN (0, 1)),
    recorded_at TEXT    NOT NULL
);
CREATE INDEX ix_usage_recorded_at ON usage_records (recorded_at);
CREATE INDEX ix_usage_line ON usage_records (line_id, recorded_at);

CREATE TABLE support_cases (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    case_number TEXT    NOT NULL UNIQUE,
    customer_id INTEGER NOT NULL REFERENCES customers (id),
    line_id     INTEGER REFERENCES mobile_lines (id),
    category    TEXT    NOT NULL CHECK (category IN ('BILLING', 'NETWORK', 'DEVICE', 'PLAN_CHANGE', 'PORTING',
                                                     'SIM_REPLACEMENT', 'SCAM_REPORT', 'ROAMING', 'GENERAL')),
    channel     TEXT    NOT NULL CHECK (channel IN ('PHONE', 'CHAT', 'STORE', 'EMAIL', 'APP')),
    subject     TEXT    NOT NULL,
    description TEXT,
    priority    TEXT    NOT NULL CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'URGENT')),
    status      TEXT    NOT NULL CHECK (status IN ('OPEN', 'IN_PROGRESS', 'PENDING_CUSTOMER', 'RESOLVED', 'CLOSED')),
    sla_due_at  TEXT    NOT NULL,
    created_at  TEXT    NOT NULL,
    resolved_at TEXT,
    CHECK ((status IN ('RESOLVED', 'CLOSED')) = (resolved_at IS NOT NULL))
);
CREATE INDEX ix_cases_customer ON support_cases (customer_id);
CREATE INDEX ix_cases_status ON support_cases (status);
CREATE INDEX ix_cases_created ON support_cases (created_at);
