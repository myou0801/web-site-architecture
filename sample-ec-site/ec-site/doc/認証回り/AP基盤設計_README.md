# AP基盤（認証）設計まとめ（完全版・確定 v2）

本ドキュメントは **AP基盤Tが提供する認証共通部品**の設計確定内容を、**省略なし**でまとめたものです。  
対象：Spring MVC（Bootなし）/ Spring Framework 6.2 / Spring Security 6.5 / MyBatis / PostgreSQL（開発はH2）

---

## 0. 全体方針（AP基盤T / 業務T 分担）

### 0.1 依存方向
- **業務T → AP基盤T** に依存（業務TはsharedServiceを呼ぶ）
- **AP基盤Tは業務Tを参照しない**

### 0.2 AP基盤Tの責務
- 認証・認可基盤（Spring Security統合、UserDetails、Handler/Interceptor、リスナー）
- 認証系DB（AUTH_*）のスキーマ/永続化（MyBatis）
- パスワード変更/管理者操作などの **sharedService** を提供
- ドメイン（Value/Entity/Policy/例外）をDDDで整備

### 0.3 業務Tの責務
- 画面（login/menu/password-change/admin）・Controller・業務Service
- 画面遷移・表示制御（メニューのロール表示切替等）
- messages.properties（AP基盤が定義するキーに文言を設定して表示）

---

## 1. 用語・命名（混同防止のため固定）

### 1.1 画面（業務T）での入力項目
- 画面項目名：**ユーザID**
- Spring Security へのパラメータ名：`userId`
- パスワードパラメータ名：`password`

### 1.2 内部不変ID（基盤）
- 内部ID名：**アカウントID**
- DBカラム：`auth_account_id`
- Java（domain）VO：`AuthAccountId`

### 1.3 ログインに使うID
- DBカラム：`user_id`（UNIQUE）
- Java（domain）VO：`UserId`

### 1.4 監査列（重要）
本設計では `*_by` は **操作者の UserId（文字列）** を保存します。  
**AuthAccountId ではありません。**（将来追加する場合は `*_by_account_id` 等で別列にする）

### 1.5 テーブル名（確定）
- マスタ：`AUTH_ACCOUNT`
- ロール：
  - `AUTH_ROLE`（ロールマスタ）
  - `AUTH_ACCOUNT_ROLE`（アカウント×ロール関連：**複数ロール対応**）
- 履歴系（名前は維持）
  - `AUTH_LOGIN_HISTORY`
  - `AUTH_PASSWORD_HISTORY`
  - `AUTH_ACCOUNT_LOCK_HISTORY`

---

## 2. 仕様（確定）

### 2.1 認証
- 認証は **ユーザID + パスワード**で行う

### 2.2 パスワードポリシー
- 最小桁数：5
- 文字種：英数字（`[0-9A-Za-z]+`）
- ログインID（ユーザID）とパスワードの完全一致は禁止
- 有効期限：90日
- 世代：3世代（過去3件と同一禁止）
- 初期パスワード（管理者初期化含む）：`password123`（暫定、後で調整可能）

### 2.3 ロックアウト
- 連続失敗 **6回**でロック
- ロック解除は **管理者操作のみ**
- パスワード初期化（resetPassword）すると **ロックも解除**
- ロック中のログインは `AUTH_LOGIN_HISTORY.result='LOCKED'` で履歴は残す  
  ただし **失敗カウントには含めない**

### 2.4 連続失敗の定義（確定）
- `AUTH_LOGIN_HISTORY.result = FAILURE` のみカウント対象
- `LOCKED / DISABLED` はカウント対象外
- 直近の `SUCCESS` 以降の `FAILURE` を数える（SUCCESSでリセット）

### 2.5 前回ログイン日時
- 「前回ログイン日時」＝ **今回ログイン成功の直前に成功していた日時（直前のSUCCESS）**
- `AuthUserDetails` に `previousLoginAt` を保持し、業務Tが共通ヘッダ等で表示できるようにする

### 2.6 パスワード変更強制
- 初回登録時 / 管理者初期化後 / 有効期限切れ（90日超過）の場合、ログイン成功後に **パスワード変更画面へ遷移**
- セッションにフラグを持たず、必要に応じて `requirementOf` で判定する

### 2.7 ロール
- 1アカウントは **複数ロール**を持てる
- 認可は Spring Security の `ROLE_` 形式へ変換して利用
  - 例：`ADMIN` → `ROLE_ADMIN`

### 2.8 監査列・論理削除（AUTH_ACCOUNT）
- `created_at/created_by/updated_at/updated_by` を保持
- 論理削除：`deleted=true`（削除時に `deleted_at/deleted_by` を埋める）
- 削除時は `enabled=false` を同時にセットする

---

## 3. 設定値（application.properties/yml で管理：確定）

推奨キー例：
- `auth.password.min-length=5`
- `auth.password.allowed-pattern=^[0-9A-Za-z]+$`
- `auth.password.expire-days=90`
- `auth.password.history-generations=3`
- `auth.lock.failure-threshold=6`
- `auth.initial-password=password123`
- `auth.default-success-url=/menu`
- `auth.pwchange.bypass-patterns=...`（PasswordChangeRequiredInterceptor の無限ループ防止）

---

## 4. DB設計（DDL：PostgreSQL / H2）

### 4.1 PostgreSQL DDL（本番）

#### 4.1.1 AUTH_ACCOUNT（現在状態 + 監査 + 論理削除）
```sql
CREATE TABLE AUTH_ACCOUNT (
  auth_account_id  BIGSERIAL PRIMARY KEY,

  user_id          VARCHAR(64) NOT NULL,
  password_hash    VARCHAR(255) NOT NULL,

  enabled          BOOLEAN NOT NULL DEFAULT TRUE,
  deleted          BOOLEAN NOT NULL DEFAULT FALSE,

  deleted_at       TIMESTAMP NULL,
  deleted_by       VARCHAR(64) NULL,

  created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_by       VARCHAR(64) NULL,

  updated_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by       VARCHAR(64) NULL
);

CREATE UNIQUE INDEX ux_auth_account_user_id
  ON AUTH_ACCOUNT(user_id);

CREATE INDEX ix_auth_account_deleted_enabled
  ON AUTH_ACCOUNT(deleted, enabled);
```

#### 4.1.2 AUTH_ROLE（ロールマスタ）
```sql
CREATE TABLE AUTH_ROLE (
  role_code    VARCHAR(64) PRIMARY KEY,
  role_name    VARCHAR(128) NOT NULL,
  enabled      BOOLEAN NOT NULL DEFAULT TRUE,
  created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX ix_auth_role_enabled
  ON AUTH_ROLE(enabled);
```

#### 4.1.3 AUTH_ACCOUNT_ROLE（複数ロール）
```sql
CREATE TABLE AUTH_ACCOUNT_ROLE (
  auth_account_id BIGINT NOT NULL,
  role_code       VARCHAR(64) NOT NULL,
  created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_by      VARCHAR(64) NULL,

  PRIMARY KEY (auth_account_id, role_code),

  CONSTRAINT fk_auth_account_role_account
    FOREIGN KEY (auth_account_id) REFERENCES AUTH_ACCOUNT(auth_account_id),

  CONSTRAINT fk_auth_account_role_role
    FOREIGN KEY (role_code) REFERENCES AUTH_ROLE(role_code)
);

CREATE INDEX ix_auth_account_role_role_code
  ON AUTH_ACCOUNT_ROLE(role_code);
```

#### 4.1.4 AUTH_PASSWORD_HISTORY（パスワード履歴：insert-only）
```sql
CREATE TABLE AUTH_PASSWORD_HISTORY (
  auth_password_history_id BIGSERIAL PRIMARY KEY,
  auth_account_id          BIGINT NOT NULL,
  change_type              VARCHAR(32) NOT NULL,   -- INITIAL_REGISTER / ADMIN_RESET / USER_CHANGE
  changed_at               TIMESTAMP NOT NULL,
  password_hash            VARCHAR(255) NOT NULL,
  created_at               TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

  CONSTRAINT fk_auth_pw_hist_account
    FOREIGN KEY (auth_account_id) REFERENCES AUTH_ACCOUNT(auth_account_id)
);

CREATE INDEX ix_auth_pw_hist_account_changed
  ON AUTH_PASSWORD_HISTORY(auth_account_id, changed_at DESC);
```

> 補足：`changed_at` は「実際に変更された時刻」、`created_at` は「履歴レコードが作られた時刻」です。  
> 通常は同一値ですが、移行や遅延登録等でも扱えるように分けています。

#### 4.1.5 AUTH_LOGIN_HISTORY（ログイン履歴：insert-only）
```sql
CREATE TABLE AUTH_LOGIN_HISTORY (
  auth_login_history_id BIGSERIAL PRIMARY KEY,
  auth_account_id       BIGINT NOT NULL,
  result                VARCHAR(32) NOT NULL, -- SUCCESS / FAILURE / LOCKED / DISABLED
  login_at              TIMESTAMP NOT NULL,
  created_at            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

  CONSTRAINT fk_auth_login_hist_account
    FOREIGN KEY (auth_account_id) REFERENCES AUTH_ACCOUNT(auth_account_id)
);

CREATE INDEX ix_auth_login_hist_account_at
  ON AUTH_LOGIN_HISTORY(auth_account_id, login_at DESC);

CREATE INDEX ix_auth_login_hist_account_result_at
  ON AUTH_LOGIN_HISTORY(auth_account_id, result, login_at DESC);
```

#### 4.1.6 AUTH_ACCOUNT_LOCK_HISTORY（ロック状態イベント：insert-only）
```sql
CREATE TABLE AUTH_ACCOUNT_LOCK_HISTORY (
  auth_account_lock_history_id BIGSERIAL PRIMARY KEY,
  auth_account_id              BIGINT NOT NULL,
  event_type                   VARCHAR(32) NOT NULL, -- LOCK / UNLOCK
  occurred_at                  TIMESTAMP NOT NULL,
  created_at                   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

  CONSTRAINT fk_auth_lock_hist_account
    FOREIGN KEY (auth_account_id) REFERENCES AUTH_ACCOUNT(auth_account_id)
);

CREATE INDEX ix_auth_lock_hist_account_at
  ON AUTH_ACCOUNT_LOCK_HISTORY(auth_account_id, occurred_at DESC);
```

---

### 4.2 H2 DDL（開発）

#### 4.2.1 AUTH_ACCOUNT
```sql
CREATE TABLE AUTH_ACCOUNT (
  auth_account_id  BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,

  user_id          VARCHAR(64) NOT NULL,
  password_hash    VARCHAR(255) NOT NULL,

  enabled          BOOLEAN NOT NULL DEFAULT TRUE,
  deleted          BOOLEAN NOT NULL DEFAULT FALSE,

  deleted_at       TIMESTAMP NULL,
  deleted_by       VARCHAR(64) NULL,

  created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_by       VARCHAR(64) NULL,

  updated_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by       VARCHAR(64) NULL
);

CREATE UNIQUE INDEX ux_auth_account_user_id
  ON AUTH_ACCOUNT(user_id);

CREATE INDEX ix_auth_account_deleted_enabled
  ON AUTH_ACCOUNT(deleted, enabled);
```

#### 4.2.2 AUTH_ROLE / AUTH_ACCOUNT_ROLE
```sql
CREATE TABLE AUTH_ROLE (
  role_code    VARCHAR(64) PRIMARY KEY,
  role_name    VARCHAR(128) NOT NULL,
  enabled      BOOLEAN NOT NULL DEFAULT TRUE,
  created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX ix_auth_role_enabled
  ON AUTH_ROLE(enabled);


CREATE TABLE AUTH_ACCOUNT_ROLE (
  auth_account_id BIGINT NOT NULL,
  role_code       VARCHAR(64) NOT NULL,
  created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_by      VARCHAR(64) NULL,
  PRIMARY KEY (auth_account_id, role_code),

  CONSTRAINT fk_auth_account_role_account
    FOREIGN KEY (auth_account_id) REFERENCES AUTH_ACCOUNT(auth_account_id),

  CONSTRAINT fk_auth_account_role_role
    FOREIGN KEY (role_code) REFERENCES AUTH_ROLE(role_code)
);

CREATE INDEX ix_auth_account_role_role_code
  ON AUTH_ACCOUNT_ROLE(role_code);
```

#### 4.2.3 AUTH_PASSWORD_HISTORY
```sql
CREATE TABLE AUTH_PASSWORD_HISTORY (
  auth_password_history_id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
  auth_account_id          BIGINT NOT NULL,
  change_type              VARCHAR(32) NOT NULL,
  changed_at               TIMESTAMP NOT NULL,
  password_hash            VARCHAR(255) NOT NULL,
  created_at               TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

  CONSTRAINT fk_auth_pw_hist_account
    FOREIGN KEY (auth_account_id) REFERENCES AUTH_ACCOUNT(auth_account_id)
);

CREATE INDEX ix_auth_pw_hist_account_changed
  ON AUTH_PASSWORD_HISTORY(auth_account_id, changed_at);
```

#### 4.2.4 AUTH_LOGIN_HISTORY
```sql
CREATE TABLE AUTH_LOGIN_HISTORY (
  auth_login_history_id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
  auth_account_id       BIGINT NOT NULL,
  result                VARCHAR(32) NOT NULL,
  login_at              TIMESTAMP NOT NULL,
  created_at            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

  CONSTRAINT fk_auth_login_hist_account
    FOREIGN KEY (auth_account_id) REFERENCES AUTH_ACCOUNT(auth_account_id)
);

CREATE INDEX ix_auth_login_hist_account_at
  ON AUTH_LOGIN_HISTORY(auth_account_id, login_at);

CREATE INDEX ix_auth_login_hist_account_result_at
  ON AUTH_LOGIN_HISTORY(auth_account_id, result, login_at);
```

#### 4.2.5 AUTH_ACCOUNT_LOCK_HISTORY
```sql
CREATE TABLE AUTH_ACCOUNT_LOCK_HISTORY (
  auth_account_lock_history_id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
  auth_account_id              BIGINT NOT NULL,
  event_type                   VARCHAR(32) NOT NULL,
  occurred_at                  TIMESTAMP NOT NULL,
  created_at                   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

  CONSTRAINT fk_auth_lock_hist_account
    FOREIGN KEY (auth_account_id) REFERENCES AUTH_ACCOUNT(auth_account_id)
);

CREATE INDEX ix_auth_lock_hist_account_at
  ON AUTH_ACCOUNT_LOCK_HISTORY(auth_account_id, occurred_at);
```

---

## 5. Flyway 構成（PostgreSQL/H2の分離：確定）

推奨：DB別に locations を分ける。

```
src/main/resources/
  db/migration/postgresql/
    V001__create_auth_schema.sql
  db/migration/h2/
    V001__create_auth_schema.sql
```

設定例（profileで切替）：
```yaml
spring:
  flyway:
    enabled: true
---
spring:
  config:
    activate:
      on-profile: postgresql
  flyway:
    locations: classpath:db/migration/postgresql
---
spring:
  config:
    activate:
      on-profile: h2
  flyway:
    locations: classpath:db/migration/h2
```

---

## 6. AP基盤のモジュール/パッケージ構成（DDD）

### 6.1 Gradleモジュール
- `presentation`
- `application`
- `domain`
- `infrastructure`

### 6.2 ルートパッケージ
- `com.myou.ec.ecsite`

### 6.3 認証（auth）配下のパッケージ例
- `com.myou.ec.ecsite.domain.auth...`
- `com.myou.ec.ecsite.application.auth...`
- `com.myou.ec.ecsite.infrastructure.auth...`
- `com.myou.ec.ecsite.presentation.auth...`

---

## 7. Domain（概念モデル）

### 7.1 Value Object（例）
- `AuthAccountId`
- `UserId`
- `RoleCode`
- `PasswordHash`
- `LoginResult`：`SUCCESS / FAILURE / LOCKED / DISABLED`
- `PasswordChangeType`：`INITIAL_REGISTER / ADMIN_RESET / USER_CHANGE`
- `LockEventType`：`LOCK / UNLOCK`

### 7.2 Entity / First-class collection（例）
- `AuthAccount`（現在状態：userId / passwordHash / enabled / deleted）
- `LoginHistory` / `LoginHistories`（連続失敗判定）
- `AccountLockEvent` / `AccountLockEvents`（ロック状態算出）
- `PasswordHistory`（履歴1件）

### 7.3 Policy（domain完結）
- `PasswordPolicy`（Rule合成。違反が1件でもあれば `PasswordPolicyViolationException`）
  - required / minLength / allowedPattern / notSameAsUserId / notReused(3世代)
- `LockPolicy`（threshold=6）

### 7.4 Domain例外（契約）
- `PasswordPolicyViolationException`
- `CurrentPasswordMismatchException`
- `AuthAccountNotFoundException`
- `AuthAccountDeletedException`
- `AuthRoleNotFoundException` / `AuthRoleDisabledException`

---

## 8. Repository / Mapper 設計（MyBatis、Record変換方針）

### 8.1 方針
- TypeHandlerは使わず、**RepositoryImplで Record ↔ Domain 変換**してMapperを呼び出す
- Record命名は `*Record`（`DbRecord` ではない）
- Repository実装クラスの命名は `*RepositoryImpl`（`Mybatis*` ではない）

### 8.2 domain.Repository I/F（概要）
- `AuthAccountRepository`
  - `Optional<AuthAccount> findActiveByUserId(UserId userId)`（認証用：`deleted=false`）
  - `Optional<AuthAccount> findById(AuthAccountId accountId)`
  - `AuthAccountId save(AuthAccount account, UserId operator)`
  - `updatePasswordHash(AuthAccountId, String hash, UserId operator)`
  - `updateEnabled(AuthAccountId, boolean enabled, UserId operator)`
  - `markDeleted(AuthAccountId, UserId operator)`（`deleted=true, enabled=false`）

- `AuthAccountRoleRepository`
  - `Set<RoleCode> findRolesByAccountId(AuthAccountId accountId)`
  - `addRole(AuthAccountId, RoleCode, UserId operator)`
  - `removeRole(AuthAccountId, RoleCode)`

- `AuthRoleRepository`
  - `boolean existsEnabled(RoleCode role)`
  - `List<AuthRole> findEnabledAll()`

- `AuthPasswordHistoryRepository`
  - `save(PasswordHistory history)`
  - `Optional<PasswordHistory> findLatestByAccountId(AuthAccountId)`
  - `List<PasswordHistory> findRecentByAccountId(AuthAccountId, int limit)`

- `AuthLoginHistoryRepository`
  - `save(LoginHistory history)`
  - `Optional<LocalDateTime> findLatestSuccessAt(AuthAccountId)`
  - `List<LoginHistory> findRecentByAccountId(AuthAccountId, int limit)`

- `AuthAccountLockHistoryRepository`
  - `AccountLockEvents findByAccountId(AuthAccountId)`
  - `save(AccountLockEvent event)`

### 8.3 MyBatis Mapper I/F（概要）
- `AuthAccountMapper`
  - `selectActiveByUserId(String userId)`（`deleted=false`）
  - `selectByAccountId(long id)`
  - `insert(AuthAccountRecord record)`（generated key取得）
  - `updatePasswordHash(...)`（updated_at/by）
  - `updateEnabled(...)`（updated_at/by）
  - `markDeleted(...)`（deleted/updated各列）

- `AuthAccountRoleMapper`
  - `selectRoleCodesByAccountId(long id)`
  - `insert(long id, String roleCode, LocalDateTime createdAt, String createdBy)`
  - `delete(long id, String roleCode)`

- `AuthRoleMapper`
  - `existsEnabled(String roleCode)`（0/1）
  - `selectEnabledAll()`

- `AuthPasswordHistoryMapper`
  - `insert(AuthPasswordHistoryRecord record)`
  - `selectLatestByAccountId(long id)`
  - `selectRecentByAccountId(long id, int limit)`

- `AuthLoginHistoryMapper`
  - `insert(AuthLoginHistoryRecord record)`
  - `selectLatestSuccessAt(long id)`
  - `selectRecentByAccountId(long id, int limit)`

- `AuthAccountLockHistoryMapper`
  - `insert(AuthAccountLockHistoryRecord record)`
  - `selectByAccountId(long id)`

---

## 9. sharedService（業務Tが呼ぶ入口）I/F（確定）

### 9.1 PasswordChangeSharedService
- confirmは業務T（presentation）で実施
- 新パスワードのチェックはAP基盤（sharedService）内で行い、違反時は `ValidationException` を投げる

```java
public interface PasswordChangeSharedService {
    PasswordChangeRequirement requirementOf(AuthAccountId accountId);

    void changePassword(AuthAccountId accountId, String currentRawPassword, String newRawPassword);
}
```

```java
public enum PasswordChangeRequirementType {
    NONE, EXPIRED, ADMIN_RESET, INITIAL_REGISTER
}

public record PasswordChangeRequirement(
        PasswordChangeRequirementType type,
        java.time.LocalDate expiredOn
) {
    public static PasswordChangeRequirement none() {
        return new PasswordChangeRequirement(PasswordChangeRequirementType.NONE, null);
    }
    public boolean required() {
        return type != PasswordChangeRequirementType.NONE;
    }
}
```

### 9.2 AuthUserContextSharedService（任意：共通ヘッダ補助）
```java
public record AuthUserContext(
        String userId,
        java.util.Set<String> roleCodes,
        java.time.LocalDateTime previousLoginAt
) {}

public interface AuthUserContextSharedService {
    AuthUserContext currentContext();
}
```

### 9.3 AuthAccountAdminSharedService（管理者操作：確定）
- 監査列のため operator（操作者）を `UserId` で受ける
- 対象指定は `AuthAccountId targetAccountId` を利用

```java
public interface AuthAccountAdminSharedService {

    AuthAccountId registerAccount(UserId newUserId, java.util.Set<RoleCode> roles, UserId operator);

    void resetPassword(AuthAccountId targetAccountId, UserId operator);

    void unlock(AuthAccountId targetAccountId, UserId operator);

    void disableAccount(AuthAccountId targetAccountId, UserId operator);

    void enableAccount(AuthAccountId targetAccountId, UserId operator);

    void addRole(AuthAccountId targetAccountId, RoleCode role, UserId operator);

    void removeRole(AuthAccountId targetAccountId, RoleCode role, UserId operator);

    void deleteAccount(AuthAccountId targetAccountId, UserId operator);
}
```

---

## 10. 管理操作の処理フロー（確定）

共通：更新系 sharedService は `@Transactional` を付与する。

### 10.1 registerAccount(newUserId, roles, operator)
1. 初期PW（password123）をハッシュ化
2. `AUTH_ACCOUNT` INSERT
   - enabled=true 固定、deleted=false 固定
   - `created_by/updated_by` は operator
3. DB採番された `auth_account_id` を取得
4. `AUTH_ACCOUNT_ROLE` INSERT（roles分）
   - created_by = operator
5. `AUTH_PASSWORD_HISTORY` INSERT
   - change_type=INITIAL_REGISTER
   - changed_at=now
   - password_hash=初期hash

例外：
- user_id重複 → `ValidationException(field=userId, key=auth.account.userId.duplicate)`

### 10.2 resetPassword(targetAccountId, operator)
1. 初期PWをハッシュ化
2. `AUTH_ACCOUNT.password_hash` UPDATE（updated_at/by=operator）
3. `AUTH_PASSWORD_HISTORY` INSERT（change_type=ADMIN_RESET）
4. `AUTH_ACCOUNT_LOCK_HISTORY` INSERT（UNLOCK）※仕様：初期化でロック解除

### 10.3 unlock(targetAccountId, operator)
- `AUTH_ACCOUNT_LOCK_HISTORY` INSERT（UNLOCK）

### 10.4 disableAccount / enableAccount
- `AUTH_ACCOUNT.enabled` を false / true に UPDATE（updated_at/by=operator）

### 10.5 addRole / removeRole
- addRole：
  - `AUTH_ROLE.enabled=true` を確認（存在しない/無効なら ValidationException）
  - `AUTH_ACCOUNT_ROLE` INSERT（PKで重複防止）
- removeRole：
  - `AUTH_ACCOUNT_ROLE` DELETE

### 10.6 deleteAccount（論理削除）
- `AUTH_ACCOUNT` を UPDATE
  - deleted=true
  - deleted_at=now
  - deleted_by=operator
  - enabled=false
  - updated_at/byも更新

---

## 11. Spring Security 統合（設計）

### 11.1 認可（URLルール：確定）
- `/login`：permitAll
- static resources（`/css/**`等）：permitAll
- `/admin/**`：`hasRole("ADMIN")`
- それ以外：authenticated

### 11.2 AuthUserDetails（基盤提供）
- 保持する情報（例）
  - `AuthAccountId accountId`
  - `UserId userId`
  - `Set<RoleCode> roles`
  - `LocalDateTime previousLoginAt`

### 11.3 AuthUserDetailsService（ロード仕様：確定）
認証時の取得手順：
1. `AUTH_ACCOUNT` を `user_id` で取得（条件：`deleted=false`）
   - 見つからない → user not found（後述：履歴に残さない）
2. `enabled=false` → Disabled相当
3. ロック状態（AccountLockEventsから算出）→ ロック中なら Locked相当
4. roles を `AUTH_ACCOUNT_ROLE` から取得し authorities へ変換
5. `previousLoginAt` は `AUTH_LOGIN_HISTORY` の SUCCESS 最新をセット

### 11.4 Success/Failure の責務分離（確定）
- 履歴登録：**イベントリスナー**で実施
  - success：`AuthenticationSuccessEvent`
  - failure：`AbstractAuthenticationFailureEvent`
- パスワード変更必須チェック：**SuccessHandler / Interceptor**で実施

### 11.5 Failureイベント → LoginResult の変換（確定）
`AbstractAuthenticationFailureEvent` の exception を見て result を決める：

| 例外（代表） | result | 失敗カウント対象 | 備考 |
|---|---|---:|---|
| `BadCredentialsException` | `FAILURE` | ✅ | PW不一致等 |
| `LockedException` | `LOCKED` | ❌ | ロック中（履歴は残す） |
| `DisabledException` | `DISABLED` | ❌ | 無効（履歴は残す） |
| その他 | 記録しない | - | 基盤例外はログへ（誤判定防止） |

### 11.6 user not found を履歴に残さない（確定）
- userIdが存在しない場合、**履歴INSERTしない**
- リスナーでは **accountIdが特定できる場合のみ** `AUTH_LOGIN_HISTORY` をINSERTする  
  （特定できない＝未存在はINSERTしない）

### 11.7 AuthAuthenticationSuccessHandler
- `SavedRequestAwareAuthenticationSuccessHandler` を extends
- `defaultSuccessUrl` を外部設定
- ログイン成功後に
  - パスワード変更必須なら `/password/change` へ優先遷移
  - そうでなければ SavedRequest or default へ

### 11.8 PasswordChangeRequiredInterceptor
- パスワード変更必須の場合、`/password/change` へ強制
- 無限ループ防止のため bypass URL パターンを外部設定で持つ

---

## 12. 例外・画面エラー連携（契約）

### 12.1 Validation例外（AP基盤→業務T）
- AP基盤 sharedService は入力チェックエラー時に `ValidationException(List<ValidationError>)` をthrow
- 業務Tは BindingResultへ変換し、Thymeleaf `th:errors` 等で表示する
- confirm不一致は業務T（presentation）でチェックし、画面で表示する

### 12.2 メッセージキー（契約：例）
ログイン：
- `auth.login.error`
- `auth.login.locked=ロックされています。管理者に連絡してロック解除してください`
- `auth.password.expired=パスワードの有効期限が切れています。変更してください。`
- `auth.login.disabled`

パスワード変更：
- `auth.password.current.invalid`
- `auth.password.new.required`
- `auth.password.new.minLength`
- `auth.password.new.alphanumeric`
- `auth.password.new.sameAsUserId`
- `auth.password.new.confirmMismatch`（業務T側）
- `auth.password.new.reuseNotAllowed`
- `auth.password.expired`
- `auth.password.resetRequired`
- `auth.password.initialRequired`

管理者：
- `auth.account.userId.duplicate=そのユーザIDは既に登録されています`
- `auth.account.notFound=対象アカウントが存在しません`
- `auth.account.deleted=既に削除されています`
- `auth.role.notFound=指定されたロールが存在しません`
- `auth.role.disabled=指定されたロールは無効です`
- `auth.account.role.duplicate=既に付与されています`

---

## 13. トランザクション境界（確定）
- 更新系 sharedService は `@Transactional`（業務TのServiceから呼ばれる前提だが、入口として自己完結）
- イベントリスナーの履歴INSERTは必要に応じてTxを持つ（「書ければ書く」方針）

---

## 14. 実装順序（AP基盤Tの作業ガイド）
1. domain（VO/Entity/Policy/例外/collection）
2. repositories（domain I/F）
3. infrastructure（Record/Mapper/RepositoryImpl）
4. sharedService（PasswordChange / Admin / Context）
5. Security（UserDetailsService / SuccessHandler / Interceptor / Listeners / Config）
6. 結合テスト（H2でログイン→履歴→ロック→PW変更強制）

---
