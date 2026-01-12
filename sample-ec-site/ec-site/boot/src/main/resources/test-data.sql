INSERT INTO AUTH_ROLE (role_code, role_name, enabled,  created_by,  updated_by)
VALUES ('ROLE_USER', 'User', TRUE,  'SYSTEM',  'SYSTEM'),
       ('ROLE_ADMIN', 'Admin', TRUE,  'SYSTEM',  'SYSTEM'),
       ('ROLE_GUEST', 'Guest', TRUE,  'SYSTEM',  'SYSTEM');

INSERT INTO AUTH_ACCOUNT (login_id, password_hash, account_status,  created_by,  updated_by)
VALUES
    ('testUser', 'pass', 'ACTIVE',  'SYSTEM',  'SYSTEM')
   ,('adminUser', 'pass', 'ACTIVE',  'SYSTEM',  'SYSTEM')
;

INSERT INTO AUTH_ACCOUNT_ROLE (auth_account_id, auth_role_id,  created_by)
VALUES (1, 1,  'SYSTEM'),
       (2, 2,  'SYSTEM');