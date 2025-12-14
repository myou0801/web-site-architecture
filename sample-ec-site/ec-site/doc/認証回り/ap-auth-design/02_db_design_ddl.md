# DB設計（DDL/インデックス/Seed/Flyway）

テーブル方針：
- 状態は `AUTH_ACCOUNT`（enabled/deleted/password_hash）
- 監査は `*_at/*_by` を持つ
- 履歴は **insert-only**（Updateを極力減らす）
  - `AUTH_LOGIN_HISTORY`
  - `AUTH_PASSWORD_HISTORY`
  - `AUTH_ACCOUNT_LOCK_HISTORY`

ロールは将来拡張を前提にマスタ＋関連で表現：
- `AUTH_ROLE`
- `AUTH_ACCOUNT_ROLE`

---

## 1. PostgreSQL DDL（本番）


```sql
-- =========================================================
-- AUTH (AP基盤) schema - PostgreSQL
-- =========================================================

-- 1) AUTH_ACCOUNT（更新あり）
CREATE TABLE AUTH_ACCOUNT
(
    auth_account_id BIGSERIAL PRIMARY KEY,

    user_id         VARCHAR(64)  NOT NULL,
    password_hash   VARCHAR(512) NOT NULL,

    enabled         BOOLEAN      NOT NULL DEFAULT TRUE,
    deleted         BOOLEAN      NOT NULL DEFAULT FALSE,

    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      VARCHAR(64)  NOT NULL,

    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by      VARCHAR(64)  NOT NULL,

    deleted_at      TIMESTAMP NULL,
    deleted_by      VARCHAR(64) NULL
);

CREATE UNIQUE INDEX ux_auth_account_user_id
    ON AUTH_ACCOUNT (user_id);

CREATE INDEX ix_auth_account_deleted_enabled
    ON AUTH_ACCOUNT (deleted, enabled);

-- 任意：deleted=true のとき deleted_at/deleted_by 必須にしたい場合
-- ALTER TABLE AUTH_ACCOUNT
--   ADD CONSTRAINT ck_auth_account_deleted_audit
--   CHECK (deleted = FALSE OR (deleted_at IS NOT NULL AND deleted_by IS NOT NULL));


-- 2) AUTH_ROLE（更新あり）
CREATE TABLE AUTH_ROLE
(
    role_code  VARCHAR(64) PRIMARY KEY,
    role_name  VARCHAR(128) NOT NULL,
    enabled    BOOLEAN      NOT NULL DEFAULT TRUE,

    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(64)  NOT NULL,

    updated_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(64)  NOT NULL
);

CREATE INDEX ix_auth_role_enabled
    ON AUTH_ROLE (enabled);


-- 3) AUTH_ACCOUNT_ROLE（insert/delete中心）
CREATE TABLE AUTH_ACCOUNT_ROLE
(
    auth_account_id BIGINT      NOT NULL,
    role_code       VARCHAR(64) NOT NULL,

    created_at      TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      VARCHAR(64) NOT NULL,

    PRIMARY KEY (auth_account_id, role_code),

    CONSTRAINT fk_auth_account_role_account
        FOREIGN KEY (auth_account_id) REFERENCES AUTH_ACCOUNT (auth_account_id),

    CONSTRAINT fk_auth_account_role_role
        FOREIGN KEY (role_code) REFERENCES AUTH_ROLE (role_code)
);

CREATE INDEX ix_auth_account_role_role_code
    ON AUTH_ACCOUNT_ROLE (role_code);


-- 4) AUTH_PASSWORD_HISTORY（insert-only）
CREATE TABLE AUTH_PASSWORD_HISTORY
(
    auth_password_history_id BIGSERIAL PRIMARY KEY,
    auth_account_id          BIGINT       NOT NULL,

    password_hash            VARCHAR(512) NOT NULL,
    change_type              VARCHAR(32)  NOT NULL, -- INITIAL_REGISTER / ADMIN_RESET / USER_CHANGE

    changed_at               TIMESTAMP    NOT NULL, -- 実際の変更時刻（移行や遅延登録も考慮）
    operated_by              VARCHAR(64) NULL,      -- 実操作した人（ADMIN_RESET/USER_CHANGEなど）

    created_at               TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by               VARCHAR(64)  NOT NULL, -- 履歴レコードを登録した主体（SYSTEM/ADMINなど）

    CONSTRAINT fk_auth_pw_hist_account
        FOREIGN KEY (auth_account_id) REFERENCES AUTH_ACCOUNT (auth_account_id)

);

CREATE INDEX ix_auth_pw_hist_account_changed_desc
    ON AUTH_PASSWORD_HISTORY (auth_account_id, changed_at DESC, auth_password_history_id DESC);


-- 5) AUTH_LOGIN_HISTORY（insert-only）※A案
CREATE TABLE AUTH_LOGIN_HISTORY
(
    auth_login_history_id BIGSERIAL PRIMARY KEY,
    auth_account_id       BIGINT      NOT NULL,

    result                VARCHAR(16) NOT NULL, -- SUCCESS / FAILURE / LOCKED / DISABLED
    login_at              TIMESTAMP   NOT NULL,

    created_at            TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by            VARCHAR(64) NOT NULL,

    CONSTRAINT fk_auth_login_hist_account
        FOREIGN KEY (auth_account_id) REFERENCES AUTH_ACCOUNT (auth_account_id)

);

CREATE INDEX ix_auth_login_hist_account_at_desc
    ON AUTH_LOGIN_HISTORY (auth_account_id, login_at DESC, auth_login_history_id DESC);

CREATE INDEX ix_auth_login_hist_account_result_at_desc
    ON AUTH_LOGIN_HISTORY (auth_account_id, result, login_at DESC);


-- 6) AUTH_ACCOUNT_LOCK_HISTORY（insert-only）
CREATE TABLE AUTH_ACCOUNT_LOCK_HISTORY
(
    auth_account_lock_history_id BIGSERIAL PRIMARY KEY,
    auth_account_id              BIGINT      NOT NULL,

    locked                       BOOLEAN     NOT NULL,
    reason                       VARCHAR(64) NOT NULL DEFAULT 'UNSPECIFIED',
    occurred_at                  TIMESTAMP   NOT NULL,

    operated_by                  VARCHAR(64) NULL,     -- 実操作した人（管理者解除など）

    created_at                   TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by                   VARCHAR(64) NOT NULL, -- 履歴レコードを登録した主体（SYSTEM/ADMINなど）

    CONSTRAINT fk_auth_lock_hist_account
        FOREIGN KEY (auth_account_id) REFERENCES AUTH_ACCOUNT (auth_account_id)
);

CREATE INDEX ix_auth_lock_hist_account_occurred_desc
    ON AUTH_ACCOUNT_LOCK_HISTORY (auth_account_id, occurred_at DESC, auth_account_lock_history_id DESC);

CREATE INDEX ix_auth_lock_hist_account_locked_occurred_desc
    ON AUTH_ACCOUNT_LOCK_HISTORY (auth_account_id, locked, occurred_at DESC);


-- 7) AUTH_ACCOUNT_EXPIRY_HISTORY（insert-only）
CREATE TABLE AUTH_ACCOUNT_EXPIRY_HISTORY
(
    auth_account_expiry_history_id BIGSERIAL PRIMARY KEY,
    auth_account_id                BIGINT      NOT NULL,

    event_type                     VARCHAR(16) NOT NULL, -- EXPIRE / UNEXPIRE
    reason                         VARCHAR(64) NOT NULL DEFAULT 'UNSPECIFIED',

    occurred_at                    TIMESTAMP   NOT NULL,
    operated_by                    VARCHAR(64) NULL,

    created_at                     TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by                     VARCHAR(64) NOT NULL,

    CONSTRAINT fk_auth_expiry_hist_account
        FOREIGN KEY (auth_account_id) REFERENCES AUTH_ACCOUNT (auth_account_id)
);

CREATE INDEX ix_auth_expiry_hist_account_occurred_desc
    ON AUTH_ACCOUNT_EXPIRY_HISTORY (auth_account_id, occurred_at DESC, auth_account_expiry_history_id DESC);

CREATE INDEX ix_auth_expiry_hist_account_event_occurred_desc
    ON AUTH_ACCOUNT_EXPIRY_HISTORY (auth_account_id, event_type, occurred_at DESC);


```

---

## 2. H2 DDL（開発）

```sql
-- =========================================================
-- AUTH (AP基盤) schema - H2 (dev)
-- =========================================================

CREATE TABLE AUTH_ACCOUNT
(
    auth_account_id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,

    user_id         VARCHAR(64)  NOT NULL,
    password_hash   VARCHAR(512) NOT NULL,

    enabled         BOOLEAN      NOT NULL DEFAULT TRUE,
    deleted         BOOLEAN      NOT NULL DEFAULT FALSE,

    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      VARCHAR(64)  NOT NULL,

    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by      VARCHAR(64)  NOT NULL,

    deleted_at      TIMESTAMP NULL,
    deleted_by      VARCHAR(64) NULL
);

CREATE UNIQUE INDEX ux_auth_account_user_id
    ON AUTH_ACCOUNT (user_id);

CREATE INDEX ix_auth_account_deleted_enabled
    ON AUTH_ACCOUNT (deleted, enabled);


CREATE TABLE AUTH_ROLE
(
    role_code  VARCHAR(64) PRIMARY KEY,
    role_name  VARCHAR(128) NOT NULL,
    enabled    BOOLEAN      NOT NULL DEFAULT TRUE,

    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(64)  NOT NULL,

    updated_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(64)  NOT NULL
);

CREATE INDEX ix_auth_role_enabled
    ON AUTH_ROLE (enabled);


CREATE TABLE AUTH_ACCOUNT_ROLE
(
    auth_account_id BIGINT      NOT NULL,
    role_code       VARCHAR(64) NOT NULL,

    created_at      TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      VARCHAR(64) NOT NULL,

    PRIMARY KEY (auth_account_id, role_code),

    CONSTRAINT fk_auth_account_role_account
        FOREIGN KEY (auth_account_id) REFERENCES AUTH_ACCOUNT (auth_account_id),

    CONSTRAINT fk_auth_account_role_role
        FOREIGN KEY (role_code) REFERENCES AUTH_ROLE (role_code)
);

CREATE INDEX ix_auth_account_role_role_code
    ON AUTH_ACCOUNT_ROLE (role_code);


CREATE TABLE AUTH_PASSWORD_HISTORY
(
    auth_password_history_id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    auth_account_id          BIGINT       NOT NULL,

    password_hash            VARCHAR(512) NOT NULL,
    change_type              VARCHAR(32)  NOT NULL,

    changed_at               TIMESTAMP    NOT NULL,
    operated_by              VARCHAR(64) NULL,

    created_at               TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by               VARCHAR(64)  NOT NULL,

    CONSTRAINT fk_auth_pw_hist_account
        FOREIGN KEY (auth_account_id) REFERENCES AUTH_ACCOUNT (auth_account_id)

);

CREATE INDEX ix_auth_pw_hist_account_changed_desc
    ON AUTH_PASSWORD_HISTORY (auth_account_id, changed_at DESC, auth_password_history_id DESC);


CREATE TABLE AUTH_LOGIN_HISTORY
(
    auth_login_history_id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    auth_account_id       BIGINT      NOT NULL,

    result                VARCHAR(16) NOT NULL,
    login_at              TIMESTAMP   NOT NULL,

    created_at            TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by            VARCHAR(64) NOT NULL,

    CONSTRAINT fk_auth_login_hist_account
        FOREIGN KEY (auth_account_id) REFERENCES AUTH_ACCOUNT (auth_account_id)

);

CREATE INDEX ix_auth_login_hist_account_at_desc
    ON AUTH_LOGIN_HISTORY (auth_account_id, login_at DESC, auth_login_history_id DESC);

CREATE INDEX ix_auth_login_hist_account_result_at_desc
    ON AUTH_LOGIN_HISTORY (auth_account_id, result, login_at DESC);


CREATE TABLE AUTH_ACCOUNT_LOCK_HISTORY
(
    auth_account_lock_history_id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    auth_account_id              BIGINT      NOT NULL,

    locked                       BOOLEAN     NOT NULL,
    reason                       VARCHAR(64) NOT NULL DEFAULT 'UNSPECIFIED',
    occurred_at                  TIMESTAMP   NOT NULL,

    operated_by                  VARCHAR(64) NULL,

    created_at                   TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by                   VARCHAR(64) NOT NULL,

    CONSTRAINT fk_auth_lock_hist_account
        FOREIGN KEY (auth_account_id) REFERENCES AUTH_ACCOUNT (auth_account_id)
);

CREATE INDEX ix_auth_lock_hist_account_occurred_desc
    ON AUTH_ACCOUNT_LOCK_HISTORY (auth_account_id, occurred_at DESC, auth_account_lock_history_id DESC);

CREATE INDEX ix_auth_lock_hist_account_locked_occurred_desc
    ON AUTH_ACCOUNT_LOCK_HISTORY (auth_account_id, locked, occurred_at DESC);


CREATE TABLE AUTH_ACCOUNT_EXPIRY_HISTORY
(
    auth_account_expiry_history_id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    auth_account_id                BIGINT      NOT NULL,

    event_type                     VARCHAR(16) NOT NULL,
    reason                         VARCHAR(64) NOT NULL DEFAULT 'UNSPECIFIED',

    occurred_at                    TIMESTAMP   NOT NULL,
    operated_by                    VARCHAR(64),

    created_at                     TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by                     VARCHAR(64) NOT NULL,

    CONSTRAINT fk_auth_expiry_hist_account
        FOREIGN KEY (auth_account_id) REFERENCES AUTH_ACCOUNT (auth_account_id)
);

CREATE INDEX ix_auth_expiry_hist_account_occurred_desc
    ON AUTH_ACCOUNT_EXPIRY_HISTORY (auth_account_id, occurred_at DESC, auth_account_expiry_history_id DESC);

CREATE INDEX ix_auth_expiry_hist_account_event_occurred_desc
    ON AUTH_ACCOUNT_EXPIRY_HISTORY (auth_account_id, event_type, occurred_at DESC);


```

---

## 3. Seed（AUTH_ROLE）

### PostgreSQL
```sql
INSERT INTO AUTH_ROLE(role_code, role_name, enabled)
VALUES
  ('ADMIN', '管理者', TRUE),
  ('USER',  '一般',  TRUE)
ON CONFLICT (role_code) DO UPDATE
  SET role_name = EXCLUDED.role_name,
      enabled   = EXCLUDED.enabled,
      updated_at = CURRENT_TIMESTAMP;
```

### H2
```sql
MERGE INTO AUTH_ROLE KEY(role_code) VALUES ('ADMIN', '管理者', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
MERGE INTO AUTH_ROLE KEY(role_code) VALUES ('USER',  '一般',  TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
```

---

## 4. Flyway 構成（DB別）

```
src/main/resources/
  db/migration/postgresql/
    V001__create_auth_schema.sql
    V002__seed_auth_role.sql
  db/migration/h2/
    V001__create_auth_schema.sql
    V002__seed_auth_role.sql
```

profile切替で locations を変更。

## 5. H2 利用時の留意点

* 開発環境では、H2 を **PostgreSQL モード** で起動することを推奨：

  ```text
  jdbc:h2:mem:ecsite;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1
  ```
