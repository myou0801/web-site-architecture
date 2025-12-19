INSERT INTO AUTH_ROLE (role_code, role_name, enabled, created_at, created_by, updated_at, updated_by)
VALUES ('ROLE_USER', 'User', TRUE, CURRENT_TIMESTAMP, 'SYSTEM', CURRENT_TIMESTAMP, 'SYSTEM'),
       ('ROLE_ADMIN', 'Admin', TRUE, CURRENT_TIMESTAMP, 'SYSTEM', CURRENT_TIMESTAMP, 'SYSTEM'),
       ('ROLE_GUEST', 'Guest', TRUE, CURRENT_TIMESTAMP, 'SYSTEM', CURRENT_TIMESTAMP, 'SYSTEM');

INSERT INTO AUTH_ACCOUNT (user_id, password_hash, account_status, created_at, created_by, updated_at, updated_by)
VALUES
    ('testUser', 'pass', 'ACTIVE', CURRENT_TIMESTAMP, 'SYSTEM', CURRENT_TIMESTAMP, 'SYSTEM')
   ,('adminUser', 'pass', 'ACTIVE', CURRENT_TIMESTAMP, 'SYSTEM', CURRENT_TIMESTAMP, 'SYSTEM')
;

INSERT INTO AUTH_ACCOUNT_ROLE (auth_account_id, role_code, created_at, created_by)
VALUES (1, 'ROLE_USER', CURRENT_TIMESTAMP, 'SYSTEM'),
       (2, 'ROLE_ADMIN', CURRENT_TIMESTAMP, 'SYSTEM');