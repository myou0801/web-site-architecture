# ER仕様（再構築DB対応）

本書は、再構築後の認証スキーマ（`AUTH_ACCOUNT`＝現在値最小、`*_HISTORY`＝insert-only）に合わせた ER 仕様です。

---

## 1. 方針（本ERの前提）
- **現在値**は `AUTH_ACCOUNT` に集約（`password_hash`, `account_status`）
- 状態変化の**事実**（ログイン、ロック、期限切れ、状態遷移、PW変更）は `*_HISTORY` に insert-only で記録
- ロック/期限切れの現在状態は「履歴の最新イベント」から導出（必要なら `AUTH_ACCOUNT_CURRENT` VIEW で集約）

---

## 2. テーブル仕様（要点）

### 2.1 AUTH_ACCOUNT
- PK：`auth_account_id`
- UK：`user_id`
- 主属性：`password_hash`, `account_status (ACTIVE/DISABLED/DELETED)`, `version`, `created_*`, `updated_*`
- 関係：
  - 1:N → `AUTH_ACCOUNT_STATUS_HISTORY`
  - 1:N → `AUTH_PASSWORD_HISTORY`
  - 1:N → `AUTH_LOGIN_HISTORY`
  - 1:N → `AUTH_ACCOUNT_LOCK_HISTORY`
  - 1:N → `AUTH_ACCOUNT_EXPIRY_HISTORY`
  - 1:N → `AUTH_ACCOUNT_ROLE`

> `account_status=DELETED` は存在しない扱い（UsernameNotFound）とする運用を推奨（ユーザ列挙対策）。

### 2.2 AUTH_ACCOUNT_STATUS_HISTORY（insert-only）
- PK：`auth_account_status_history_id`
- FK：`auth_account_id` → AUTH_ACCOUNT
- 属性：`from_status`, `to_status`, `reason`, `occurred_at`, `operated_by`
- 用途：有効/無効/削除など status 遷移の事実

### 2.3 AUTH_PASSWORD_HISTORY（insert-only）
- PK：`auth_password_history_id`
- FK：`auth_account_id`
- 属性：`change_type`, `changed_at`, `password_hash`, `operated_by`
- 用途：世代禁止、運用調査

### 2.4 AUTH_LOGIN_HISTORY（insert-only）
- PK：`auth_login_history_id`
- FK：`auth_account_id`
- 属性：`result`, `login_at`, （任意）`remote_ip`, `user_agent`
- result：`SUCCESS / FAILURE / LOCKED / DISABLED / EXPIRED`
- 用途：前回ログイン、連続失敗カウント、期限切れ判定の lastSuccessAt

### 2.5 AUTH_ACCOUNT_LOCK_HISTORY（insert-only）
- PK：`auth_account_lock_history_id`
- FK：`auth_account_id`
- 属性：`event_type (LOCK/UNLOCK)`, `reason`, `occurred_at`, `operated_by`
- 用途：ロック/解除の事実（現在ロック状態は最新イベントで導出）

### 2.6 AUTH_ACCOUNT_EXPIRY_HISTORY（insert-only）
- PK：`auth_account_expiry_history_id`
- FK：`auth_account_id`
- 属性：`event_type (EXPIRE/UNEXPIRE)`, `reason`, `occurred_at`, `operated_by`
- 用途：期限切れ/解除の事実（現在期限切れは最新イベント + 90日ルールで導出）

### 2.7 AUTH_ROLE / AUTH_ACCOUNT_ROLE
- AUTH_ROLE
  - PK：`auth_role_id`
  - UK：`role_code`
  - 属性：`role_name`, `enabled`
- AUTH_ACCOUNT_ROLE
  - PK：`auth_account_role_id`
  - FK：`auth_account_id` → AUTH_ACCOUNT
  - FK：`auth_role_id` → AUTH_ROLE
  - UK：`(auth_account_id, auth_role_id)`

---

## 3. 認証判定における参照観点（実装上の要点）
- AUTH_ACCOUNT：存在（user_id）と `account_status`
- ロック：LOCK_HISTORY の最新イベント
- 期限切れ：EXPIRY_HISTORY の最新イベント + baseAt（lastSuccessAt / lastUnexpireAt）
- ロール：ACCOUNT_ROLE + ROLE(enabled=true)

以上。
