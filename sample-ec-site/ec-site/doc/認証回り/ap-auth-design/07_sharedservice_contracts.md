# sharedService（I/F・実装テンプレ・Validation契約）

本書は「AUTH_ACCOUNT＝現在値（最小）＋履歴はinsert-only」の構成における、sharedServiceのI/Fと実装契約（例外/Validation/履歴追記）を定義します。

---

## 1. sharedService I/F（確定）

### PasswordChangeSharedService
```java
public interface PasswordChangeSharedService {
    PasswordChangeRequirement requirementOf(AuthAccountId accountId);
    void changePassword(AuthAccountId accountId, String currentRawPassword, String newRawPassword);
}
```

### AuthAccountAdminSharedService
```java
public interface AuthAccountAdminSharedService {
    AuthAccountId registerAccount(UserId newUserId, java.util.Set<RoleCode> roles, UserId operator);

    void resetPassword(AuthAccountId targetAccountId, UserId operator);

    /** ロック解除は管理者のみ：UNLOCKイベントを履歴へ追記 */
    void unlock(AuthAccountId targetAccountId, UserId operator);

    void disableAccount(AuthAccountId targetAccountId, UserId operator);
    void enableAccount(AuthAccountId targetAccountId, UserId operator);

    void addRole(AuthAccountId targetAccountId, RoleCode role, UserId operator);
    void removeRole(AuthAccountId targetAccountId, RoleCode role, UserId operator);

    void deleteAccount(AuthAccountId targetAccountId, UserId operator);
}
```

---

## 2. 実装テンプレ（トランザクションと履歴追記）

### 2.1 registerAccount（初期作成）
- 目的：`AUTH_ACCOUNT` を作成し、初期パスワードを設定し、ロールを付与し、履歴を追記する
- Tx：単一Txで完結

手順（推奨）：
1) `newUserId` の重複は **DBのUNIQUE制約**に任せる（例外をValidationへ変換）
2) `AUTH_ACCOUNT` INSERT（`account_status=ACTIVE`）し、accountId取得
3) `AUTH_PASSWORD_HISTORY` に `INITIAL` を追記
4) `AUTH_ACCOUNT_STATUS_HISTORY` に `NONE -> ACTIVE` を追記（初期状態の事実として残す）
5) `roles` の各 `role_code` を `AUTH_ROLE` から解決し `AUTH_ACCOUNT_ROLE` へ付与

### 2.2 disableAccount / enableAccount / deleteAccount（account_status 遷移）
- 目的：`AUTH_ACCOUNT.account_status` を更新し、その事実を `AUTH_ACCOUNT_STATUS_HISTORY` に追記
- Tx：単一Tx

共通手順（推奨）：
1) `AUTH_ACCOUNT` を `findById`（存在/現在status/version取得）
2) 遷移可否を判定
3) `AUTH_ACCOUNT` を `version` 一致条件で UPDATE（楽観ロック）
4) `AUTH_ACCOUNT_STATUS_HISTORY` を insert-only で追記

遷移ルール（最小）：
- `ACTIVE -> DISABLED`（disable）
- `DISABLED -> ACTIVE`（enable）
- `ACTIVE/DISABLED -> DELETED`（delete：以後はログイン不可）
- `DELETED` からの戻しは不可（要件が出るまで非対応）

### 2.3 unlock（管理者解除のみ）
- 目的：`AUTH_ACCOUNT_LOCK_HISTORY` に `UNLOCK` を追記する
- `AUTH_ACCOUNT` は更新しない（カラム増回避の方針）
- idempotent を許容（非ロックでも UNLOCK を追記してよい）

手順（推奨）：
1) `AUTH_ACCOUNT` の存在確認（無ければ NotFound）
2) `AUTH_ACCOUNT_LOCK_HISTORY` に `UNLOCK` を追記（reasonは任意）

### 2.4 resetPassword（管理者リセット）
- 目的：`AUTH_ACCOUNT.password_hash` を更新し、`AUTH_PASSWORD_HISTORY` に追記する
- Tx：単一Tx

手順（推奨）：
1) `AUTH_ACCOUNT` を `findById`（status/version取得）
2) `account_status != ACTIVE` の場合の扱いは要件で決める（基本：DELETEDは不可、DISABLEDは可/不可どちらでも）
3) 新パスワード生成 or 受領（仕様に従う）→ hash化
4) `AUTH_ACCOUNT` 更新（楽観ロック）
5) `AUTH_PASSWORD_HISTORY` に `ADMIN_RESET` を追記

### 2.5 addRole / removeRole
- 目的：`RoleCode -> auth_role_id` を解決し、関連を追加/削除

手順（推奨）：
1) account存在確認（DELETEDは不可）
2) role_code存在確認（無効ロールは不可）
3) `AUTH_ACCOUNT_ROLE` insert/delete

---

## 3. Validation契約（AP基盤→業務T）

- field名は業務Tフォームのプロパティ名に一致させる
- messageKeyは messages.properties に定義
- args は `{0},{1}…` に差し込む想定

フィールド名（固定）：
- login：`userId`, `password`
- password-change：`currentPassword`, `newPassword`, `confirmPassword`（confirmは業務T）
- admin register：`userId`, `roles`

最低限の messageKey：
- `auth.login.failed`
- `auth.login.locked`
- `auth.login.disabled`
- `auth.login.deleted`
- `auth.login.expired`

- `auth.password.current.invalid`
- `auth.password.new.minLength`
- `auth.password.new.complexity`
- `auth.password.new.sameAsUserId`
- `auth.password.new.reuseNotAllowed`

- `auth.role.required`
- `auth.role.notFound`
- `auth.role.disabled`
- `auth.account.userId.duplicate`
- `auth.account.notFound`
- `auth.account.deleted`
- `auth.account.status.invalidTransition`

