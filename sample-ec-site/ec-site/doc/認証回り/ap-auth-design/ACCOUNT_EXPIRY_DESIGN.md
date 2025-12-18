# アカウント期限切れ（EXPIRED）設計（確定版：再構築DB対応）

## 0. 目的 / 背景
- 90日以上ログイン成功がないユーザを「アカウント期限切れ」として扱い、ログインを拒否する。
- 期限切れは「無効（account_status=DISABLED）」とは別軸として扱う。
- Updateを増やしたくない方針に合わせ、期限切れ状態は **insert-only のイベント履歴**から導出する。

対象スタック（前提）
- Java 25 / Spring Framework 6.2 / Spring Security 6.5
- Spring MVC（non-Boot）+ MyBatis + Thymeleaf
- PostgreSQL（開発時 H2）

---

## 1. 用語
- **期限切れ（Expired）**：長期間未ログインにより、アカウントが「期限切れ」状態と判断されること
- **EXPIREイベント**：期限切れに遷移したことを表すイベント
- **UNEXPIREイベント**：期限切れ解除（管理者操作）を表すイベント
- **有効/無効/削除（account_status）**：`AUTH_ACCOUNT.account_status` による管理状態（ACTIVE/DISABLED/DELETED）

---

## 2. 認証まわりとの関係（重要）
- UserDetails の状態チェック（概念）
  - `account_status != ACTIVE` → （DISABLED 相当）ログイン拒否
  - accountNonLocked=false → LOCKED としてログイン拒否
  - **accountNonExpired=false → EXPIRED（期限切れ）としてログイン拒否**
- PW有効期限は「認証成功後にPW変更画面へ遷移」仕様のため、credentialsNonExpired は利用しない（常にtrue運用）。

> `account_status=DELETED` は存在しない扱い（UsernameNotFoundException）とする運用を推奨（ユーザ列挙対策）。

---

## 3. ログイン履歴 result（確定）
AUTH_LOGIN_HISTORY.result を以下に統一する：
- SUCCESS
- FAILURE
- LOCKED
- DISABLED
- **EXPIRED**

連続失敗ロックのカウント対象：
- FAILURE のみ
- LOCKED / DISABLED / EXPIRED はカウントに含めない

---

## 4. テーブル（insert-only）：AUTH_ACCOUNT_EXPIRY_HISTORY

### 4.1 役割
- 期限切れ状態を「フラグ」ではなく「イベント」から導出する。
- 管理者操作で期限切れ解除（UNEXPIRE）を積めるようにする。

### 4.2 event_type / reason
- event_type：EXPIRE / UNEXPIRE
- reason（例）
  - INACTIVE_90D（90日未ログインによる期限切れ）
  - ADMIN_ENABLE（管理者による有効化→期限切れ解除）

> DDL は `02_db_design_ddl.md`（再構築版）を正とする。

---

## 5. 期限切れ判定（90日ルール）

### 5.1 基準日時
- lastSuccessAt：AUTH_LOGIN_HISTORY(result='SUCCESS') の最新 login_at
- lastUnexpireAt：AUTH_ACCOUNT_EXPIRY_HISTORY(event_type='UNEXPIRE') の最新 occurred_at

baseAt = max(lastSuccessAt, lastUnexpireAt)

### 5.2 条件
- baseAt が存在し、かつ now - baseAt >= 90日 → 期限切れ
- baseAt が存在しない（成功も解除もない） → 初回等を想定し期限切れにしない

---

## 6. 処理フロー

### 6.1 AuthUserDetailsService（認証時）
1) AUTH_ACCOUNT取得
2) `account_status=DELETED` → UsernameNotFound（存在しない扱い）
3) `account_status=DISABLED` → enabled=false（Disabled扱い）
4) ロック最新イベントが LOCK → accountNonLocked=false
5) expiryEvents取得（AUTH_ACCOUNT_EXPIRY_HISTORY）
6) expiryEvents.isExpired()==true → accountNonExpired=false で返す（ログイン拒否）
7) 期限切れでない場合、baseAt を計算
8) 90日超なら EXPIRE を insert（reason=INACTIVE_90D）し、accountNonExpired=false で返す

### 6.2 Failureリスナー（ログイン履歴）
例外種別→result
- AccountExpiredException → EXPIRED
- LockedException → LOCKED
- DisabledException → DISABLED
- BadCredentialsException 等 → FAILURE

### 6.3 管理者の有効化（ACTIVE化）操作
- 管理画面で `account_status` を `ACTIVE` に戻す場合、
  - もし expiryEvents.isExpired()==true なら UNEXPIRE を insert（reason=ADMIN_ENABLE）する

---

## 7. 分担（本件）
- AP基盤T：DDL/Domain/Infrastructure/ExpiryService/Failure result=EXPIRED対応
- 業務T：管理画面の有効化操作、ログイン画面メッセージ

以上。
