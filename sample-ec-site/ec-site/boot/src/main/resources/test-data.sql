INSERT INTO AUTH_ROLE (role_code, role_name, enabled, created_at, created_by, updated_at, updated_by)
VALUES ('ROLE_USER', 'User', TRUE, CURRENT_TIMESTAMP, 'SYSTEM', CURRENT_TIMESTAMP, 'SYSTEM'),
       ('ROLE_ADMIN', 'Admin', TRUE, CURRENT_TIMESTAMP, 'SYSTEM', CURRENT_TIMESTAMP, 'SYSTEM'),
       ('ROLE_GUEST', 'Guest', TRUE, CURRENT_TIMESTAMP, 'SYSTEM', CURRENT_TIMESTAMP, 'SYSTEM');

INSERT INTO AUTH_ACCOUNT (user_id, password_hash, enabled, deleted, created_at, created_by, updated_at,
                          updated_by, deleted_at, deleted_by)
VALUES
    ('testUser', 'pass', TRUE, FALSE, CURRENT_TIMESTAMP, 'SYSTEM', CURRENT_TIMESTAMP, 'SYSTEM', NULL,NULL)
   ,('adminUser', 'pass', TRUE, FALSE, CURRENT_TIMESTAMP, 'SYSTEM', CURRENT_TIMESTAMP, 'SYSTEM', NULL,NULL)
;

INSERT INTO AUTH_ACCOUNT_ROLE (auth_account_id, role_code, created_at, created_by)
VALUES (1, 'ROLE_USER', CURRENT_TIMESTAMP, 'SYSTEM'),
       (2, 'ROLE_ADMIN', CURRENT_TIMESTAMP, 'SYSTEM');