-- V3: Create session and attendance tables

CREATE TABLE session (
    id                   UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id            UUID        NOT NULL REFERENCES tenant (id),
    class_id             UUID        NOT NULL REFERENCES class (id),
    session_date         DATE        NOT NULL,
    start_time           TIME,
    end_time             TIME,
    topic                TEXT,
    cancelled_by_teacher BOOLEAN     NOT NULL DEFAULT false,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_session_tenant     ON session (tenant_id);
CREATE INDEX idx_session_class      ON session (class_id);
CREATE INDEX idx_session_date       ON session (class_id, session_date);

CREATE TABLE attendance (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id  UUID        NOT NULL REFERENCES tenant (id),
    session_id UUID        NOT NULL REFERENCES session (id),
    student_id UUID        NOT NULL REFERENCES student (id),
    status     VARCHAR(50) NOT NULL DEFAULT 'PRESENT',   -- PRESENT | ABSENT | LATE
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (session_id, student_id)
);

CREATE INDEX idx_attendance_tenant  ON attendance (tenant_id);
CREATE INDEX idx_attendance_session ON attendance (session_id);
CREATE INDEX idx_attendance_student ON attendance (student_id);
