# Infrastructure（Record/Mapper I/F/RepositoryImpl/Clock/楽観ロック/現在状態導出）

本書は「AUTH_ACCOUNT＝現在値（最小）＋履歴はinsert-only」の構成における、インフラ層（MyBatis/Repository）設計方針をまとめます。

方針：
- TypeHandlerは原則使わず、RepositoryImplで **Record ↔ Domain** 変換してMapperを呼ぶ
- Record命名：`*Record`（DB行）
- Repository実装命名：`*RepositoryImpl`
- 履歴系は **insert-only** のため、Repositoryは `append*`（追記）を中心に提供

---

## 1. ID採番とRecord immutability

Javaの `record` は immutable なため、MyBatisの `useGeneratedKeys`（keyPropertyへ書き戻し）と相性が悪い。

採用案（確定）
- `AUTH_ACCOUNT` INSERT後、同一Tx内で `user_id`（UNIQUE）で再SELECTし `auth_account_id` を取得する

代替案（必要になったら）
- INSERT専用の mutable DTO（setter付き）を導入し、generatedKeysを使う
- DB方言に依存する `selectKey`（sequence）を使う

---

## 2. Clock（時刻供給）

AP基盤で `Clock` をBean提供（確定）

```java
@Bean
public Clock clock() {
    return Clock.systemDefaultZone();
}
```

---

## 3. 楽観ロック（version）と「0件更新」契約

### 3.1 対象
- `AUTH_ACCOUNT.version` を楽観ロックに利用する
- 更新対象は以下に限定する
  - `password_hash` の更新（パスワード変更/リセット）
  - `account_status` の更新（有効/無効/論理削除）

### 3.2 SQL契約
- UPDATEは `WHERE auth_account_id=? AND version=?` を必須とし、更新時に `version = version + 1`
- **0件更新の場合**：
  - 前提（存在・削除状態）が変わった、または並行更新が起きた可能性が高い
  - RepositoryImpl は `OptimisticLockException`（独自例外で可）に変換して application へ伝搬する

### 3.3 sharedService側の扱い（推奨）
- 先に `findById` で存在・状態を判定したうえで更新
- それでも 0件更新なら「競合」として扱い、
  - 管理画面：操作やり直しを促す（409相当）
  - バッチ：リトライまたはスキップ（要件次第）

---

## 4. “現在状態”の導出（ロック/期限切れ）

本構成では `AUTH_ACCOUNT` に `locked` / `expired` を持たない。
代わりに `*_HISTORY` の最新イベントから現在状態を導出する。

### 4.1 ロック判定（管理者解除のみ仕様）
- `AUTH_ACCOUNT_LOCK_HISTORY` の最新 `event_type` が `LOCK` ならロック中
- `UNLOCK` または履歴なしなら非ロック

### 4.2 期限切れ判定
- `AUTH_ACCOUNT_EXPIRY_HISTORY` の最新 `event_type` が `EXPIRE` なら期限切れ
- `UNEXPIRE` または履歴なしなら期限切れではない

### 4.3 往復削減（任意）
- PostgreSQLでは `AUTH_ACCOUNT_CURRENT` VIEW（LATERAL）を作成し、
  - `AUTH_ACCOUNT` ＋ 最新LOCK/EXPIRE を 1回のSELECTで取得可能

---

## 5. Repositoryの責務分離（推奨I/F）

### 5.1 AuthAccountRepository
- `Optional<AuthAccount> findByUserId(UserId userId)`
- `Optional<AuthAccount> findById(AuthAccountId id)`
- `AuthAccountId insertNew(UserId userId, PasswordHash hash, AccountStatus status, Operator op)`
- `void updatePassword(AuthAccountId id, PasswordHash newHash, int expectedVersion, Operator op)`
- `void updateStatus(AuthAccountId id, AccountStatus newStatus, int expectedVersion, Operator op)`

### 5.2 History Repositories（append-only）
- `AuthAccountStatusHistoryRepository.append(from,to,occurredAt,operator,reason)`
- `AuthPasswordHistoryRepository.append(changeType,changedAt,hash,operator)`
- `AuthLoginHistoryRepository.append(result,loginAt,remoteIp,userAgent)`
- `AuthAccountLockHistoryRepository.append(eventType,occurredAt,operator,reason)`
- `AuthAccountExpiryHistoryRepository.append(eventType,occurredAt,operator,reason)`

### 5.3 Role Repositories
- `AuthRoleRepository.findRoleIdByCode(RoleCode)`
- `AuthAccountRoleRepository.add(accountId, roleId, operator)`
- `AuthAccountRoleRepository.remove(accountId, roleId)`

---

## 6. トランザクション境界（基本方針）

- sharedService（application）が `@Transactional` 境界を持つ
- 例：状態変更の標準パターン
  1) `AUTH_ACCOUNT` 読み取り（version取得）
  2) `AUTH_ACCOUNT` 更新（楽観ロック）
  3) `AUTH_ACCOUNT_STATUS_HISTORY` 追記（insert-only）

- ロック解除（管理者のみ）
  - `AUTH_ACCOUNT_LOCK_HISTORY` に `UNLOCK` を追記するだけ（AUTH_ACCOUNT更新なし）

