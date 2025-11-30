# Domain設計（Value/Entity/Policy/例外）

方針：
- domainservice は極力作らない（ポリシー・エンティティ・ファーストコレクションに寄せる）
- application/sharedService が domain を組み立てて操作する
- domain は application を参照しない（ValidationException等は application で変換）

---

## 1. Value Object（例）
- `AuthAccountId`（long）
- `UserId`（String）
- `RoleCode`（String）
- `PasswordHash`（String）
- `LoginResult`：`SUCCESS / FAILURE / LOCKED / DISABLED`
- `PasswordChangeType`：`INITIAL_REGISTER / ADMIN_RESET / USER_CHANGE`
- `LockEventType`：`LOCK / UNLOCK`

---

## 2. Entity / First-class collection（例）

### 2.1 AuthAccount
- 属性：accountId / userId / passwordHash / enabled / deleted
- 更新は application/sharedService が repository 経由で行う（Updateは必要最小）

### 2.2 LoginHistory / LoginHistories
- `LoginHistories.isOverThreshold(threshold)` は
  - 直近から見て `SUCCESS` が出るまでの `FAILURE` を数える
  - `LOCKED/DISABLED` はカウント対象外（クエリで除外）

### 2.3 AccountLockEvent / AccountLockEvents
- 方針A（割り切り）：LOCKイベントの重複は許容
- 状態判定は “最新イベントのみ”（最新=LOCK → ロック中）

### 2.4 PasswordHistory
- `changed_at` ベースで履歴の最新・世代チェックを実施

---

## 3. Policy（ポリシーパターン）

### 3.1 PasswordPolicy（Rule合成）
- ルールを複数登録し、違反が1件でもあれば例外（policyがthrow）
- ViolationCode enum は作らない（ルールごとに messageKey を持つ）

例ルール：
- minLength(5)
- allowedPattern([0-9A-Za-z]+)
- notSameAsUserId
- notReused(N=3)（履歴hashとの一致禁止）

### 3.2 LockPolicy
- threshold=6（設定値）

---

## 4. Domain例外（例）
- `PasswordPolicyViolationException`（複数違反を保持）
- `CurrentPasswordMismatchException`
- `AuthAccountNotFoundException`
- `AuthAccountDeletedException`
- `AuthRoleNotFoundException`
- `AuthRoleDisabledException`

> application/sharedService が domain例外を catch して `ValidationException` に変換する。
