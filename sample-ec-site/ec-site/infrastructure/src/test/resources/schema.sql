/* =========================================================
 * H2（開発用）
 * ========================================================= */

-- ==============
-- Tables
-- ==============

CREATE TABLE AUTH_ACCOUNT
(
    auth_account_id BIGINT AUTO_INCREMENT PRIMARY KEY,

    user_id         VARCHAR(64)  NOT NULL,
    password_hash   VARCHAR(512) NOT NULL,

    -- ライフサイクル状態（enabled/deleted を統合）
    account_status  VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',

    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      VARCHAR(64)  NOT NULL,

    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by      VARCHAR(64)  NOT NULL,

    -- 同時更新対策（任意だが推奨）
    version         INTEGER      NOT NULL DEFAULT 0,

    CONSTRAINT ux_auth_account_user_id UNIQUE (user_id),
    CONSTRAINT ck_auth_account_status CHECK (account_status IN ('ACTIVE', 'DISABLED', 'DELETED'))
);
CREATE TABLE AUTH_ROLE
(
    auth_role_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_code    VARCHAR(64)  NOT NULL,
    role_name    VARCHAR(128) NOT NULL,
    enabled      BOOLEAN      NOT NULL DEFAULT TRUE,

    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by   VARCHAR(64)  NOT NULL,
    updated_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by   VARCHAR(64)  NOT NULL,

    CONSTRAINT ux_auth_role_code UNIQUE (role_code)
);
CREATE TABLE AUTH_ACCOUNT_ROLE
(
    auth_account_role_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    auth_account_id      BIGINT      NOT NULL,
    auth_role_id         BIGINT      NOT NULL,

    created_at           TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by           VARCHAR(64) NOT NULL,

    CONSTRAINT ux_auth_account_role UNIQUE (auth_account_id, auth_role_id),
    CONSTRAINT fk_auth_account_role_account FOREIGN KEY (auth_account_id) REFERENCES AUTH_ACCOUNT (auth_account_id),
    CONSTRAINT fk_auth_account_role_role FOREIGN KEY (auth_role_id) REFERENCES AUTH_ROLE (auth_role_id)
);
CREATE TABLE AUTH_PASSWORD_HISTORY
(
    auth_password_history_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    auth_account_id          BIGINT       NOT NULL,

    password_hash            VARCHAR(512) NOT NULL,
    change_type              VARCHAR(32)  NOT NULL, -- INITIAL / USER_CHANGE / ADMIN_RESET 等

    changed_at               TIMESTAMP    NOT NULL,
    operated_by              VARCHAR(64)  NOT NULL, -- user_id / admin_id / SYSTEM

    created_at               TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by               VARCHAR(64)  NOT NULL,

    CONSTRAINT fk_auth_pwd_hist_account FOREIGN KEY (auth_account_id) REFERENCES AUTH_ACCOUNT (auth_account_id)
);
CREATE TABLE AUTH_LOGIN_HISTORY
(
    auth_login_history_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    auth_account_id       BIGINT      NOT NULL,

    result                VARCHAR(16) NOT NULL, -- SUCCESS / FAILURE / LOCKED / DISABLED / DELETED / EXPIRED 等
    login_at              TIMESTAMP   NOT NULL,

    created_at            TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by            VARCHAR(64) NOT NULL,

    CONSTRAINT fk_auth_login_hist_account FOREIGN KEY (auth_account_id) REFERENCES AUTH_ACCOUNT (auth_account_id)
);
CREATE TABLE AUTH_ACCOUNT_LOCK_HISTORY
(
    auth_account_lock_history_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    auth_account_id              BIGINT      NOT NULL,
    locked                       BOOLEAN     NOT NULL,
    reason                       VARCHAR(64) NOT NULL,

    occurred_at                  TIMESTAMP   NOT NULL,
    operated_by                  VARCHAR(64) NOT NULL, -- admin_id / SYSTEM

    created_at                   TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by                   VARCHAR(64) NOT NULL,

    CONSTRAINT fk_auth_lock_hist_account FOREIGN KEY (auth_account_id) REFERENCES AUTH_ACCOUNT (auth_account_id)
);
CREATE TABLE AUTH_ACCOUNT_EXPIRY_HISTORY
(
    auth_account_expiry_history_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    auth_account_id                BIGINT      NOT NULL,

    event_type                     VARCHAR(16) NOT NULL, -- EXPIRE / UNEXPIRE
    reason                         VARCHAR(64) NOT NULL,

    occurred_at                    TIMESTAMP   NOT NULL,
    operated_by                    VARCHAR(64) NOT NULL, -- admin_id / SYSTEM

    created_at                     TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by                     VARCHAR(64) NOT NULL,

    CONSTRAINT fk_auth_expiry_hist_account FOREIGN KEY (auth_account_id) REFERENCES AUTH_ACCOUNT (auth_account_id),
    CONSTRAINT ck_auth_expiry_hist_type CHECK (event_type IN ('EXPIRE', 'UNEXPIRE'))
);
CREATE TABLE AUTH_ACCOUNT_STATUS_HISTORY
(
    auth_account_status_history_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    auth_account_id                BIGINT      NOT NULL,

    from_status                    VARCHAR(16) NOT NULL,
    to_status                      VARCHAR(16) NOT NULL,
    reason                         VARCHAR(64) NOT NULL,

    occurred_at                    TIMESTAMP   NOT NULL,
    operated_by                    VARCHAR(64) NOT NULL,

    created_at                     TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by                     VARCHAR(64) NOT NULL,

    CONSTRAINT fk_auth_status_hist_account FOREIGN KEY (auth_account_id) REFERENCES AUTH_ACCOUNT (auth_account_id),
    CONSTRAINT ck_auth_status_hist_from CHECK (from_status IN ('ACTIVE', 'DISABLED', 'DELETED')),
    CONSTRAINT ck_auth_status_hist_to CHECK (to_status IN ('ACTIVE', 'DISABLED', 'DELETED'))
);

-- ==============
-- Indexes
-- ==============

CREATE INDEX ix_auth_account_status ON AUTH_ACCOUNT (account_status);
CREATE INDEX ix_auth_account_role_account ON AUTH_ACCOUNT_ROLE (auth_account_id);
CREATE INDEX ix_auth_pwd_hist_account_changed_desc
    ON AUTH_PASSWORD_HISTORY (auth_account_id, changed_at, auth_password_history_id);
CREATE INDEX ix_auth_login_hist_account_at_desc
    ON AUTH_LOGIN_HISTORY (auth_account_id, login_at, auth_login_history_id);
CREATE INDEX ix_auth_login_hist_account_result_at_desc
    ON AUTH_LOGIN_HISTORY (auth_account_id, result, login_at);
CREATE INDEX ix_auth_lock_hist_account_occurred_desc
    ON AUTH_ACCOUNT_LOCK_HISTORY (auth_account_id, occurred_at, auth_account_lock_history_id);
CREATE INDEX ix_auth_lock_hist_account_locked_occurred_desc
    ON AUTH_ACCOUNT_LOCK_HISTORY (auth_account_id, locked, occurred_at);
CREATE INDEX ix_auth_expiry_hist_account_occurred_desc
    ON AUTH_ACCOUNT_EXPIRY_HISTORY (auth_account_id, occurred_at, auth_account_expiry_history_id);
CREATE INDEX ix_auth_expiry_hist_account_event_occurred_desc
    ON AUTH_ACCOUNT_EXPIRY_HISTORY (auth_account_id, event_type, occurred_at);
CREATE INDEX ix_auth_status_hist_account_occurred_desc
    ON AUTH_ACCOUNT_STATUS_HISTORY (auth_account_id, occurred_at, auth_account_status_history_id);


-- ==============
-- Views（Query sharedService）
-- ==============

/* --- Query sharedService 用 VIEW（H2：row_number） --- */

DROP VIEW IF EXISTS AUTH_ACCOUNT_LOCK_LATEST_V;
CREATE VIEW AUTH_ACCOUNT_LOCK_LATEST_V AS
SELECT auth_account_id, locked, occurred_at
FROM (SELECT auth_account_id,
             locked,
             occurred_at,
             ROW_NUMBER() OVER (
           PARTITION BY auth_account_id
           ORDER BY occurred_at DESC, auth_account_lock_history_id DESC
         ) AS rn
      FROM AUTH_ACCOUNT_LOCK_HISTORY) t
WHERE t.rn = 1;

DROP VIEW IF EXISTS AUTH_ACCOUNT_EXPIRY_LATEST_V;
CREATE VIEW AUTH_ACCOUNT_EXPIRY_LATEST_V AS
SELECT auth_account_id, event_type, occurred_at
FROM (SELECT auth_account_id,
             event_type,
             occurred_at,
             ROW_NUMBER() OVER (
           PARTITION BY auth_account_id
           ORDER BY occurred_at DESC, auth_account_expiry_history_id DESC
         ) AS rn
      FROM AUTH_ACCOUNT_EXPIRY_HISTORY) t
WHERE t.rn = 1;

/* 最終ログイン（SUCCESSの最新）※不要なら削除 */
DROP VIEW IF EXISTS AUTH_ACCOUNT_LAST_LOGIN_V;
CREATE VIEW AUTH_ACCOUNT_LAST_LOGIN_V AS
SELECT auth_account_id, login_at
FROM (SELECT auth_account_id,
             login_at,
             ROW_NUMBER() OVER (
           PARTITION BY auth_account_id
           ORDER BY login_at DESC, auth_login_history_id DESC
         ) AS rn
      FROM AUTH_LOGIN_HISTORY
      WHERE result = 'SUCCESS') t
WHERE t.rn = 1;

DROP VIEW IF EXISTS AUTH_ACCOUNT_CURRENT_V;
CREATE VIEW AUTH_ACCOUNT_CURRENT_V AS
SELECT a.auth_account_id,
       a.user_id,
       a.account_status,
       COALESCE(l.locked, FALSE)                  AS locked,
       COALESCE((e.event_type = 'EXPIRE'), FALSE) AS expired,
       ll.login_at                                AS last_login_at,
       a.created_at,
       a.updated_at
FROM AUTH_ACCOUNT a
         LEFT JOIN AUTH_ACCOUNT_LOCK_LATEST_V l ON l.auth_account_id = a.auth_account_id
         LEFT JOIN AUTH_ACCOUNT_EXPIRY_LATEST_V e ON e.auth_account_id = a.auth_account_id
         LEFT JOIN AUTH_ACCOUNT_LAST_LOGIN_V ll ON ll.auth_account_id = a.auth_account_id;

DROP VIEW IF EXISTS AUTH_ACCOUNT_ROLE_V;
CREATE VIEW AUTH_ACCOUNT_ROLE_V AS
SELECT aar.auth_account_id,
       r.role_code
FROM AUTH_ACCOUNT_ROLE aar
         JOIN AUTH_ROLE r
              ON r.auth_role_id = aar.auth_role_id
WHERE r.enabled = TRUE;