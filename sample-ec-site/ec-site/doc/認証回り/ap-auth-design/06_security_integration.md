# Spring Security統合（SecurityConfig / UserDetails / Handler / Interceptor）

本書は「DB再構築版（`AUTH_ACCOUNT`＝現在値最小、`*_HISTORY`＝insert-only）」に合わせた Spring Security 統合設計です。

- `AUTH_ACCOUNT`：`account_status`（ACTIVE/DISABLED/DELETED）と `password_hash` を保持（更新あり）
- ロック：`AUTH_ACCOUNT_LOCK_HISTORY` の最新イベント（LOCK/UNLOCK）から導出
- 期限切れ：`AUTH_ACCOUNT_EXPIRY_HISTORY` の最新イベント（EXPIRE/UNEXPIRE）＋ 90日未ログイン判定（必要なら EXPIRE を insert）

---

## 1. SecurityConfig（Bootなし前提：確定）

### FilterChain（要点）
- `formLogin`
  - loginPage：`/login`
  - loginProcessingUrl：`/login`（POST）
  - usernameParameter：`userId`
  - passwordParameter：`password`
  - successHandler：`AuthAuthenticationSuccessHandler`（SavedRequestAwareをextends）
  - failureUrl：`/login?error`
- 認可
  - `/login`/静的：permitAll
  - `/admin/**`：hasRole("ADMIN")
  - others：authenticated
- logout
  - `/logout` → `/login?logout`
- CSRF：基本は有効（Thymeleafフォームで対応）

### Event publisher（確定）
- `DefaultAuthenticationEventPublisher` を Bean 登録し、Success/Failure イベントリスナーで履歴登録する

---
## 2. 認証時ロード（UserDetailsService：確定）

### 2.1 取得する情報
- アカウント現在値（必須）：`AUTH_ACCOUNT`
  - `user_id`, `password_hash`, `account_status`
- ロック現在状態（必要）：`AUTH_ACCOUNT_LOCK_HISTORY` の最新 `event_type`
- 期限切れ現在状態（必要）：`AUTH_ACCOUNT_EXPIRY_HISTORY` の最新 `event_type` と 90日未ログイン判定
- ロール（必要）：`AUTH_ACCOUNT_ROLE` → `AUTH_ROLE(enabled=true)` の `role_code`
- lastSuccessAt（期限切れ判定に使用）：`AUTH_LOGIN_HISTORY(result='SUCCESS')` の最新 `login_at`

> 参照回数を抑えたい場合は PostgreSQL の `AUTH_ACCOUNT_CURRENT` VIEW を利用し、
> 「アカウント＋最新ロック/期限イベント」を1回で取る構成にしてもよい（H2では別SQLで代替）。

### 2.2 優先順位（ログイン拒否理由の評価順）
- `DELETED`（=存在しない扱い） > `DISABLED` > `LOCKED` > `EXPIRED` > `BAD_CREDENTIALS`

※ `DELETED` はユーザ列挙を避けるため、基本は `UsernameNotFoundException` として扱う（画面は一律エラー表示）。

### 2.3 Spring Security の各フラグへの割当
`UserDetails` への割当は以下。

- `enabled`：`account_status == 'ACTIVE'` の場合のみ `true`（それ以外は `false`）
- `accountNonLocked`：最新ロックイベントが `LOCK` の場合は `false`
- `accountNonExpired`：期限切れ判定が `true` の場合は `false`
- `credentialsNonExpired`：本システムでは利用しない（常に `true`）

### 2.4 期限切れ判定（90日ルール）
`AccountExpiryService` で判定し、必要なら `EXPIRE` イベントを insert する。

- lastSuccessAt：`AUTH_LOGIN_HISTORY(result='SUCCESS')` の最新
- lastUnexpireAt：`AUTH_ACCOUNT_EXPIRY_HISTORY(event_type='UNEXPIRE')` の最新
- baseAt = max(lastSuccessAt, lastUnexpireAt)
- baseAt が存在し、かつ now-baseAt >= 90日 → 期限切れ
  - 期限切れになったら `AUTH_ACCOUNT_EXPIRY_HISTORY(EXPIRE, reason=INACTIVE_90D)` を insert
  - `accountNonExpired=false` として返す

---

## 3. SuccessHandler / Interceptor（確定）

### 3.1 SuccessHandler
- SavedRequest を優先して遷移（SavedRequestAware）
- 追加判定（例）
  - パスワード変更が必須ならパスワード変更画面へリダイレクト

### 3.2 Interceptor（直リンク対策）
- ログイン後の全リクエストで「変更必須」などの条件を強制する場合に利用
- bypass
  - `auth.pwchange.bypass-patterns`（例：`/login,/logout,/password/change/**,/css/**,...`）

---

## 4. 実装の最小依存（Mapper/Service）
- `AuthAccountMapper.selectByUserId(userId)`
- `AuthAccountLockHistoryMapper.selectLatestEvent(authAccountId)`（LOCK/UNLOCK）
- `AuthAccountExpiryHistoryMapper.selectLatestEvent(authAccountId)`（EXPIRE/UNEXPIRE）
- `AuthLoginHistoryMapper.selectLatestSuccessLoginAt(authAccountId)`
- `AuthRoleMapper.selectRoleCodesByAccountId(authAccountId)`
- `AccountExpiryService`（90日判定 + EXPIRE insert）

以上。
