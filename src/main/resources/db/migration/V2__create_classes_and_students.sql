-- V2: Create class, student, and class_student tables

CREATE TABLE class (
    id               UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id        UUID         NOT NULL REFERENCES tenant (id),
    name             VARCHAR(255) NOT NULL,
    subject          VARCHAR(255),
    teacher_id       UUID         NOT NULL REFERENCES app_user (id),
    rate_per_session NUMERIC(12, 2) NOT NULL DEFAULT 0,
    status           VARCHAR(50)  NOT NULL    DEFAULT 'ACTIVE',   -- ACTIVE | ARCHIVED
    created_at       TIMESTAMPTZ  NOT NULL    DEFAULT now()
);

CREATE INDEX idx_class_tenant  ON class (tenant_id);
CREATE INDEX idx_class_teacher ON class (teacher_id);

CREATE TABLE student (
    id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id    UUID         NOT NULL REFERENCES tenant (id),
    full_name    VARCHAR(255) NOT NULL,
    phone        VARCHAR(50),
    parent_phone VARCHAR(50),
    notes        TEXT,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_student_tenant ON student (tenant_id);

-- Many-to-many enrollment
CREATE TABLE class_student (
    class_id   UUID        NOT NULL REFERENCES class (id),
    student_id UUID        NOT NULL REFERENCES student (id),
    enrolled_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (class_id, student_id)
);

CREATE INDEX idx_class_student_student ON class_student (student_id);
