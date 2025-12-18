# Domain設計（Value/Entity/Policy/例外）

方針：
- domainservice は極力作らない（ポリシー・エンティティ・ファーストコレクションに寄せる）
- application/sharedService が domain を組み立てて操作する
- domain は application を参照しない（ValidationException等は application で変換）
- DB再構築版に合わせ、`AUTH_ACCOUNT` は **最小の現在値** を持ち、
  それ以外の「状態遷移の事実」は `*_HISTORY`（insert-only）で表現する

---

## 1. Value Object（例）
- `AuthAccountId`（long）
- `UserId`（String）
- `RoleCode`（String）
- `PasswordHash`（String）

- `AccountStatus`：`ACTIVE / DISABLED / DELETED`
- `LoginResult`：`SUCCESS / FAILURE / LOCKED / DISABLED / EXPIRED`
- `PasswordChangeType`：`INITIAL_REGISTER / ADMIN_RESET / USER_CHANGE`
- `LockEventType`：`LOCK / UNLOCK`
- `ExpiryEventType`：`EXPIRE / UNEXPIRE`

---

## 2. Entity / First-class collection（例）

### 2.1 AuthAccount
- 属性：`authAccountId / userId / passwordHash / accountStatus / version`
- 「ロック中」「期限切れ」は `AUTH_ACCOUNT` の列では保持しない（履歴・ポリシーから導出）
- 更新は application/sharedService が repository 経由で行う（Updateは必要最小）

### 2.2 LoginHistory / LoginHistories
- `LoginHistories.countConsecutiveFailures()` は
  - 直近から見て `SUCCESS` が出るまでの `FAILURE` を数える
  - `LOCKED/DISABLED/EXPIRED` はカウント対象外（SQL側で除外するのが基本）

### 2.3 AccountLockEvent / AccountLockEvents
- 状態判定は「最新イベントのみ」
  - 最新=LOCK → ロック中
  - 最新=UNLOCK or 存在なし → 非ロック

### 2.4 AccountExpiryEvent / AccountExpiryEvents
- 状態判定は「最新イベントのみ」
  - 最新=EXPIRE → 期限切れ
  - 最新=UNEXPIRE or 存在なし → 非期限切れ

### 2.5 PasswordHistory / PasswordHistories
- `changed_at` ベースで履歴の最新・世代チェックを実施

---

## 3. Policy（ポリシーパターン）

### 3.1 PasswordPolicy（Rule合成）
- ルールを複数登録し、違反が1件でもあれば例外（policyがthrow）
- ViolationCode enum は作らない（ルールごとに messageKey を持つ）

例ルール：
- minLength(12)
- charClassAtLeast(3)
- allowedSymbols(#$%()+=?@*[]{}|\\)
- notSameAsUserId
- notReused(N=3)（履歴hashとの一致禁止）

### 3.2 LockPolicy
- threshold（設定値）
- 連続失敗は `AUTH_LOGIN_HISTORY` から導出
- ロックの事実は `AUTH_ACCOUNT_LOCK_HISTORY(LOCK)` を insert

### 3.3 AccountExpiryPolicy
- inactiveDays=90（設定値）
- baseAt = max(lastSuccessAt, lastUnexpireAt)
- baseAt が存在し、かつ now-baseAt >= inactiveDays → 期限切れ
- 期限切れの事実は `AUTH_ACCOUNT_EXPIRY_HISTORY(EXPIRE)` を insert

---

## 4. Domain例外（例）
- `PasswordPolicyViolationException`（複数違反を保持）
- `CurrentPasswordMismatchException`

- `AuthAccountNotFoundException`
- `AuthAccountDeletedException`
- `AuthAccountDisabledException`
- `AuthAccountLockedException`
- `AuthAccountExpiredException`

- `AuthRoleNotFoundException`
- `AuthRoleDisabledException`

> application/sharedService が domain例外を catch して `ValidationException` 等に変換する。
