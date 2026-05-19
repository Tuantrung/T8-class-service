-- V6: Seed development data (active only when spring.profiles.active=dev)
-- This migration is guarded: only apply via Flyway locations override in dev profile.
-- In production, flyway.locations should NOT include this file.
-- BCrypt hash below is for password: "password123"

INSERT INTO tenant (id, name)
VALUES ('00000000-0000-0000-0000-000000000001', 'Demo Tutoring Center')
ON CONFLICT DO NOTHING;

INSERT INTO app_user (id, tenant_id, email, password_hash, full_name, role)
VALUES (
    '00000000-0000-0000-0000-000000000010',
    '00000000-0000-0000-0000-000000000001',
    'admin@demo.com',
    '$2a$12$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',  -- password123
    'Demo Admin',
    'ADMIN'
) ON CONFLICT DO NOTHING;

INSERT INTO app_user (id, tenant_id, email, password_hash, full_name, role)
VALUES (
    '00000000-0000-0000-0000-000000000011',
    '00000000-0000-0000-0000-000000000001',
    'teacher@demo.com',
    '$2a$12$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',  -- password123
    'Demo Teacher',
    'TEACHER'
) ON CONFLICT DO NOTHING;

INSERT INTO class (id, tenant_id, name, subject, teacher_id, rate_per_session, status)
VALUES (
    '00000000-0000-0000-0000-000000000020',
    '00000000-0000-0000-0000-000000000001',
    'Math Grade 10 - Group A',
    'Mathematics',
    '00000000-0000-0000-0000-000000000011',
    150000,
    'ACTIVE'
) ON CONFLICT DO NOTHING;

INSERT INTO student (id, tenant_id, full_name, phone, parent_phone)
VALUES
    ('00000000-0000-0000-0000-000000000030', '00000000-0000-0000-0000-000000000001', 'Nguyen Van A', '0901234567', '0912345678'),
    ('00000000-0000-0000-0000-000000000031', '00000000-0000-0000-0000-000000000001', 'Tran Thi B',  '0902345678', '0923456789'),
    ('00000000-0000-0000-0000-000000000032', '00000000-0000-0000-0000-000000000001', 'Le Van C',    '0903456789', '0934567890')
ON CONFLICT DO NOTHING;

INSERT INTO class_student (class_id, student_id)
VALUES
    ('00000000-0000-0000-0000-000000000020', '00000000-0000-0000-0000-000000000030'),
    ('00000000-0000-0000-0000-000000000020', '00000000-0000-0000-0000-000000000031'),
    ('00000000-0000-0000-0000-000000000020', '00000000-0000-0000-0000-000000000032')
ON CONFLICT DO NOTHING;
