# Spring Security統合（SecurityConfig / UserDetails / Handler / Interceptor）

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
- 認可：
  - `/login`/静的：permitAll
  - `/admin/**`：hasRole("ADMIN")
  - others：authenticated
- logout：
  - `/logout` → `/login?logout`
- CSRF：
  - **基本：有効のまま**（Thymeleafフォームで対応）

### Event publisher（確定）
- `DefaultAuthenticationEventPublisher` をBean登録し、Success/Failureイベントリスナーで履歴登録する

---

## 2. 認証時ロード（確定）
1. `AUTH_ACCOUNT` を `user_id` で取得（`deleted=false`）
2. `enabled=false` → DISABLED
3. ロック：LockHistory最新がLOCK → LOCKED
4. roles：AUTH_ACCOUNT_ROLE（AUTH_ROLE.enabled=trueのみ）
5. previousLoginAt：LOGIN_HISTORY直近SUCCESS

優先順位（確定）：
- deleted（対象外） > disabled > locked > bad credentials

---

## 3. SuccessHandler / Interceptor（確定）
- SuccessHandler：ログイン直後の1回目遷移（SavedRequest/defaultSuccessUrl/パスワード変更優先）
- Interceptor：ログイン後のすべてで変更必須なら強制（直リンク対策）

bypass：
- `auth.pwchange.bypass-patterns`（例：`/login,/logout,/password/change/**,/css/**,...`）
