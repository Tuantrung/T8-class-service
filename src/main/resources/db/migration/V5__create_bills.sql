-- V5: Create bill table

CREATE TABLE bill (
    id                UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id         UUID           NOT NULL REFERENCES tenant (id),
    student_id        UUID           NOT NULL REFERENCES student (id),
    class_id          UUID           NOT NULL REFERENCES class (id),
    billing_month     DATE           NOT NULL,   -- stored as first day of month: 2024-03-01
    sessions_total    INTEGER        NOT NULL DEFAULT 0,
    sessions_attended INTEGER        NOT NULL DEFAULT 0,
    rate_per_session  NUMERIC(12, 2) NOT NULL,
    total_amount      NUMERIC(12, 2) NOT NULL,
    status            VARCHAR(50)    NOT NULL DEFAULT 'DRAFT',   -- DRAFT | ISSUED | PAID
    pdf_path          VARCHAR(500),
    created_at        TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ    NOT NULL DEFAULT now(),
    UNIQUE (tenant_id, student_id, class_id, billing_month)
);

CREATE INDEX idx_bill_tenant  ON bill (tenant_id);
CREATE INDEX idx_bill_student ON bill (student_id);
CREATE INDEX idx_bill_month   ON bill (tenant_id, billing_month);
