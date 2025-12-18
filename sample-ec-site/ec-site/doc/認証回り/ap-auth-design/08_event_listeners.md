# 認証イベントリスナー（Success/Failure：履歴登録・ロック判定）

本書は「DB再構築版（`AUTH_ACCOUNT`＝現在値最小、`*_HISTORY`＝insert-only）」に合わせた認証イベントリスナー設計です。

- ログイン試行の事実：`AUTH_LOGIN_HISTORY`（insert-only）
- ロック状態：`AUTH_ACCOUNT_LOCK_HISTORY` の最新イベント（LOCK/UNLOCK）で決定（解除は管理者のみ）
- 期限切れ：`AccountExpiryService`（UserDetailsService側）で判定し、必要なら `AUTH_ACCOUNT_EXPIRY_HISTORY(EXPIRE)` を insert

---

## 1. Successリスナー（AuthenticationSuccessEvent）

### 1.1 目的
- ログイン成功の事実を `AUTH_LOGIN_HISTORY(SUCCESS)` として保存

### 1.2 処理フロー
1. `userId = event.getAuthentication().getName()`
2. `AUTH_ACCOUNT` を `user_id` で取得
   - 取得不可、または `account_status='DELETED'` の場合は **何もしない**（ユーザ列挙を避ける方針）
3. `AUTH_LOGIN_HISTORY` に `SUCCESS` を insert
   - `remote_ip` / `user_agent` は取得できる場合のみ保存（不要なら NULL）

### 1.3 トランザクション
- `@Transactional` 推奨（履歴INSERTが失敗しても認証成否には影響させない）

---

## 2. Failureリスナー（AbstractAuthenticationFailureEvent）

### 2.1 目的
- ログイン失敗の種別を `AUTH_LOGIN_HISTORY` に保存
- （FAILURE のみ）連続失敗ポリシーでロック判定し、必要なら `AUTH_ACCOUNT_LOCK_HISTORY(LOCK)` を insert

### 2.2 result へのマッピング（推奨）
`event.getException()` の型から以下に変換する。

- `LockedException` → `LOCKED`
- `DisabledException` → `DISABLED`
- `AccountExpiredException` → `EXPIRED`
- `BadCredentialsException` 等 → `FAILURE`

> 画面メッセージは一律にしてもよい（内部履歴の result と UI 表示は分離する）。

### 2.3 処理フロー
1. `userId = event.getAuthentication().getName()`
2. `AUTH_ACCOUNT` を `user_id` で取得
   - 取得不可、または `account_status='DELETED'` の場合は **何もしない**（ユーザ列挙を避ける方針）
3. 例外型から `result` を決定し、`AUTH_LOGIN_HISTORY` に insert
4. `result == 'FAILURE'` の場合のみ、連続失敗ロックを評価
   - 直近 N 件の `AUTH_LOGIN_HISTORY(result in ('SUCCESS','FAILURE'))` を取得
   - 「最後に SUCCESS が出現するまでの FAILURE 連続数」をカウント
   - 閾値以上の場合、最新ロックイベントを確認し、未ロックなら `AUTH_ACCOUNT_LOCK_HISTORY(LOCK)` を insert

### 2.4 連続失敗ロックの実装メモ
- カウント対象は `FAILURE` のみ（`LOCKED/DISABLED/EXPIRED` は含めない）
- ロック判定は「二重ロック」を避けるため、`AUTH_ACCOUNT_LOCK_HISTORY` の最新が `LOCK` なら何もしない
- ロックの `operated_by` は `SYSTEM` を推奨（自動ロック）

---

## 3. 例外取り扱い（重要）
- リスナー内例外は認証成否に影響させない（握って warn ログ）
- ただし DB 障害時に履歴が欠落する可能性があるため、運用監視（アプリログ/メトリクス）は別途検討

---

## 4. 依存 Mapper（最小）
- `AuthAccountMapper.selectByUserId(userId)`
- `AuthLoginHistoryMapper.insertHistory(...)`
- `AuthLoginHistoryMapper.selectRecentSuccessFailure(authAccountId, limit)`
- `AuthAccountLockHistoryMapper.selectLatestEvent(authAccountId)`
- `AuthAccountLockHistoryMapper.insertHistory(...)`

以上。
