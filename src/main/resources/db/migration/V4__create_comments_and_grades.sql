-- V4: Create comment and grade tables

CREATE TABLE comment (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id  UUID        NOT NULL REFERENCES tenant (id),
    session_id UUID        NOT NULL REFERENCES session (id),
    student_id UUID        NOT NULL REFERENCES student (id),
    author_id  UUID        NOT NULL REFERENCES app_user (id),
    body       TEXT        NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (session_id, student_id)   -- one comment per student per session
);

CREATE INDEX idx_comment_tenant  ON comment (tenant_id);
CREATE INDEX idx_comment_session ON comment (session_id);

CREATE TABLE grade (
    id         UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id  UUID           NOT NULL REFERENCES tenant (id),
    class_id   UUID           NOT NULL REFERENCES class (id),
    student_id UUID           NOT NULL REFERENCES student (id),
    exam_name  VARCHAR(255)   NOT NULL,
    exam_date  DATE,
    score      NUMERIC(6, 2),
    max_score  NUMERIC(6, 2),
    notes      TEXT,
    created_at TIMESTAMPTZ    NOT NULL DEFAULT now(),
    UNIQUE (class_id, student_id, exam_name)
);

CREATE INDEX idx_grade_tenant        ON grade (tenant_id);
CREATE INDEX idx_grade_class_student ON grade (class_id, student_id);
