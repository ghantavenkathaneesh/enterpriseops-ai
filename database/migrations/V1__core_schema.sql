-- EnterpriseOps AI: core schema (PostgreSQL). All timestamps are UTC (TIMESTAMPTZ).

CREATE TABLE roles (
    id   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(30) NOT NULL UNIQUE CHECK (name IN ('ADMIN','MANAGER','SUPPORT_AGENT','EMPLOYEE'))
);

CREATE TABLE users (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email         VARCHAR(255) NOT NULL UNIQUE,
    full_name     VARCHAR(150) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    role_id       BIGINT NOT NULL REFERENCES roles(id),
    active        BOOLEAN NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_users_role ON users(role_id);

CREATE TABLE assets (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    asset_tag  VARCHAR(40) NOT NULL UNIQUE,
    name       VARCHAR(150) NOT NULL,
    asset_type VARCHAR(30) NOT NULL CHECK (asset_type IN ('SERVER','LAPTOP','NETWORK','DATABASE','OTHER')),
    status     VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','MAINTENANCE','RETIRED')),
    owner_id   BIGINT REFERENCES users(id) ON DELETE SET NULL,
    location   VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_assets_owner ON assets(owner_id);

CREATE TABLE sla_policies (
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    priority           VARCHAR(10) NOT NULL UNIQUE CHECK (priority IN ('LOW','MEDIUM','HIGH','CRITICAL')),
    response_minutes   INT NOT NULL CHECK (response_minutes > 0),
    resolution_minutes INT NOT NULL CHECK (resolution_minutes > 0)
);

CREATE TABLE tickets (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    title       VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    category    VARCHAR(30) NOT NULL DEFAULT 'other',
    priority    VARCHAR(10) NOT NULL DEFAULT 'MEDIUM' CHECK (priority IN ('LOW','MEDIUM','HIGH','CRITICAL')),
    status      VARCHAR(15) NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN','IN_PROGRESS','ON_HOLD','RESOLVED','CLOSED')),
    reporter_id BIGINT NOT NULL REFERENCES users(id),
    assignee_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    asset_id    BIGINT REFERENCES assets(id) ON DELETE SET NULL,
    due_at      TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    resolved_at TIMESTAMPTZ,
    CHECK (resolved_at IS NULL OR resolved_at >= created_at)
);
CREATE INDEX idx_tickets_status     ON tickets(status);
CREATE INDEX idx_tickets_assignee   ON tickets(assignee_id);
CREATE INDEX idx_tickets_reporter   ON tickets(reporter_id);
CREATE INDEX idx_tickets_created_at ON tickets(created_at);
CREATE INDEX idx_tickets_category   ON tickets(category);

CREATE TABLE ticket_comments (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    ticket_id  BIGINT NOT NULL REFERENCES tickets(id) ON DELETE CASCADE,
    author_id  BIGINT NOT NULL REFERENCES users(id),
    body       TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_comments_ticket ON ticket_comments(ticket_id);

CREATE TABLE incidents (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    title            VARCHAR(200) NOT NULL,
    description      TEXT NOT NULL,
    severity         VARCHAR(10) NOT NULL CHECK (severity IN ('LOW','MEDIUM','HIGH','CRITICAL')),
    status           VARCHAR(15) NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN','INVESTIGATING','RESOLVED','CLOSED')),
    source           VARCHAR(15) NOT NULL DEFAULT 'MANUAL' CHECK (source IN ('MANUAL','MONITORING')),
    assignee_id      BIGINT REFERENCES users(id) ON DELETE SET NULL,
    ticket_id        BIGINT REFERENCES tickets(id) ON DELETE SET NULL,
    asset_id         BIGINT REFERENCES assets(id) ON DELETE SET NULL,
    resolution_notes TEXT,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    resolved_at      TIMESTAMPTZ
);
CREATE INDEX idx_incidents_status   ON incidents(status);
CREATE INDEX idx_incidents_severity ON incidents(severity);
-- Dedupe guard: at most one active monitoring incident per asset.
CREATE UNIQUE INDEX uq_incident_active_monitoring
    ON incidents(asset_id) WHERE source = 'MONITORING' AND status IN ('OPEN','INVESTIGATING');

CREATE TABLE system_metrics (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    asset_id     BIGINT NOT NULL REFERENCES assets(id) ON DELETE CASCADE,
    cpu_percent  NUMERIC(5,2) NOT NULL CHECK (cpu_percent  BETWEEN 0 AND 100),
    ram_percent  NUMERIC(5,2) NOT NULL CHECK (ram_percent  BETWEEN 0 AND 100),
    disk_percent NUMERIC(5,2) NOT NULL CHECK (disk_percent BETWEEN 0 AND 100),
    network_mbps NUMERIC(10,2) NOT NULL CHECK (network_mbps >= 0),
    simulated    BOOLEAN NOT NULL DEFAULT TRUE,
    recorded_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_metrics_asset_time ON system_metrics(asset_id, recorded_at DESC);

CREATE TABLE anomaly_events (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    metric_id        BIGINT NOT NULL REFERENCES system_metrics(id) ON DELETE CASCADE,
    asset_id         BIGINT NOT NULL REFERENCES assets(id) ON DELETE CASCADE,
    detection_source VARCHAR(15) NOT NULL CHECK (detection_source IN ('ML','THRESHOLD')),
    score            NUMERIC(8,4),
    reason           VARCHAR(255) NOT NULL,
    incident_id      BIGINT REFERENCES incidents(id) ON DELETE SET NULL,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_anomaly_asset ON anomaly_events(asset_id, created_at DESC);

CREATE TABLE ai_predictions (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    kind            VARCHAR(15) NOT NULL CHECK (kind IN ('CLASSIFICATION','ANOMALY')),
    ticket_id       BIGINT REFERENCES tickets(id) ON DELETE SET NULL,
    input_text      TEXT,
    predicted_label VARCHAR(50) NOT NULL,
    confidence      NUMERIC(5,4),
    model_version   VARCHAR(50) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_predictions_kind ON ai_predictions(kind);

CREATE TABLE notifications (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id    BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type       VARCHAR(30) NOT NULL,
    message    VARCHAR(500) NOT NULL,
    link       VARCHAR(200),
    is_read    BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_notifications_user ON notifications(user_id, is_read);

CREATE TABLE audit_logs (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    actor_id    BIGINT REFERENCES users(id) ON DELETE SET NULL,
    action      VARCHAR(40) NOT NULL,
    entity_type VARCHAR(40) NOT NULL,
    entity_id   BIGINT,
    details     VARCHAR(500),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_audit_created ON audit_logs(created_at DESC);
CREATE INDEX idx_audit_entity  ON audit_logs(entity_type, entity_id);
