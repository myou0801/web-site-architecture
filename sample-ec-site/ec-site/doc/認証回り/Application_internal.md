LoginSharedUseCase / LockControlSharedUseCase は、いままで作ってきた
**DomainService＋Repository をまとめて「ログイン時の一連処理」をカプセル化するユースケース層**です。

場所イメージ：

```text
com.myou.ec.ecsite.application.auth.sharedservice.internal
 ├─ LoginSharedUseCase
 └─ LockControlSharedUseCase
```

として設計してみます。

---

# 1. LoginSharedUseCase の設計

## 1.1 役割

* Spring Security の **認証成功時**に呼ばれるユースケース
* やること

    1. `loginId` から `AuthUser` を取得
    2. **前回ログイン日時**を取得
    3. **ログイン成功履歴**を登録（AUTH_LOGIN_HISTORY）
    4. **パスワード変更が必要かどうか**判定

        * 初回登録／管理者初期化直後
        * 有効期限切れ（90日超）
    5. SuccessHandler が使える結果 DTO を返却

        * 「パスワード変更画面へ飛ばすか／メニューへ飛ばすか」
        * 「前回ログイン日時」

## 1.2 インタフェース案

```java
package com.myou.ec.ecsite.application.auth.sharedservice.internal;

import com.myou.ec.ecsite.domain.auth.model.value.LoginId;

public interface LoginSharedUseCase {

    LoginSuccessResult handleLoginSuccess(LoginId loginId);
}
```

戻り値 DTO（record）：

```java
package com.myou.ec.ecsite.application.auth.sharedservice.internal;

import java.time.LocalDateTime;

/**
 * 認証成功後のフロー判定結果。
 */
public record LoginSuccessResult(
        long authUserId,
        String loginId,
        LocalDateTime loginAt,
        LocalDateTime previousLoginAt,   // null の場合は「前回なし（初回ログイン）」
        boolean passwordExpired,        // 有効期限切れか
        boolean mustChangePassword      // 画面遷移: true -> /password/change, false -> /menu
) {}
```

* SuccessHandler では **`mustChangePassword` を見てリダイレクト先を決定**します。
* `previousLoginAt` はメニュー画面などで「前回ログイン日時」として表示。

## 1.3 利用する DomainService / Repository

依存：

* `AuthUserRepository`
* `AuthPolicyDomainService`
* `LoginHistoryDomainService`
* `PasswordHistoryDomainService`

    * ※ここで「直近の PasswordHistory（changeType 含む）」を取りたいので
      `PasswordHistoryDomainService` に `Optional<PasswordHistory> findLastHistory(AuthUserId)` を追加して使う想定です。

## 1.4 処理フロー

`handleLoginSuccess(loginId)` の中身（ざっくり）：

1. 現在日時 `now` を取得
2. `AuthUserRepository.findByLoginId(loginId)` でユーザ取得
3. `LoginHistoryDomainService.findPreviousSuccessLoginAt(authUserId)` で前回成功日時を取得
4. `LoginHistoryDomainService.recordSuccess(authUserId, now)` で SUCCESS 履歴を登録
5. `PasswordHistoryDomainService.findLastHistory(authUserId)` で最後の履歴を取得

    * `changeType` が `INITIAL_REGISTER` / `ADMIN_RESET` なら「初回ログイン扱い」で必ず変更
    * `changedAt` から有効期限切れかどうかを `AuthPolicyDomainService.isPasswordExpired()` で判定
6. `mustChangePassword = (changeType == INITIAL_REGISTER/ADMIN_RESET) || passwordExpired`
7. `LoginSuccessResult` を組み立てて返却

## 1.5 実装イメージ

```java
package com.myou.ec.ecsite.application.auth.sharedservice.internal;

import com.myou.ec.ecsite.domain.auth.domainservice.AuthPolicyDomainService;
import com.myou.ec.ecsite.domain.auth.domainservice.LoginHistoryDomainService;
import com.myou.ec.ecsite.domain.auth.domainservice.PasswordHistoryDomainService;
import com.myou.ec.ecsite.domain.auth.model.AuthUser;
import com.myou.ec.ecsite.domain.auth.model.PasswordHistory;
import com.myou.ec.ecsite.domain.auth.model.value.AuthUserId;
import com.myou.ec.ecsite.domain.auth.model.value.LoginId;
import com.myou.ec.ecsite.domain.auth.model.value.PasswordChangeType;
import com.myou.ec.ecsite.domain.auth.repository.AuthUserRepository;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;

public class LoginSharedUseCaseImpl implements LoginSharedUseCase {

    private final AuthUserRepository authUserRepository;
    private final AuthPolicyDomainService authPolicyDomainService;
    private final LoginHistoryDomainService loginHistoryDomainService;
    private final PasswordHistoryDomainService passwordHistoryDomainService;
    private final Clock clock;

    public LoginSharedUseCaseImpl(AuthUserRepository authUserRepository,
                                  AuthPolicyDomainService authPolicyDomainService,
                                  LoginHistoryDomainService loginHistoryDomainService,
                                  PasswordHistoryDomainService passwordHistoryDomainService,
                                  Clock clock) {
        this.authUserRepository = authUserRepository;
        this.authPolicyDomainService = authPolicyDomainService;
        this.loginHistoryDomainService = loginHistoryDomainService;
        this.passwordHistoryDomainService = passwordHistoryDomainService;
        this.clock = clock;
    }

    @Override
    public LoginSuccessResult handleLoginSuccess(LoginId loginId) {
        LocalDateTime now = LocalDateTime.now(clock);

        AuthUser user = authUserRepository.findByLoginId(loginId)
                .orElseThrow(() -> new IllegalStateException("user not found: " + loginId.value()));

        AuthUserId authUserId = user.id();

        // 1. 前回ログイン日時取得
        Optional<LocalDateTime> previousLoginAtOpt =
                loginHistoryDomainService.findPreviousSuccessLoginAt(authUserId);

        // 2. 成功履歴登録
        loginHistoryDomainService.recordSuccess(authUserId, now);

        // 3. パスワード変更必要判定
        Optional<PasswordHistory> lastHistoryOpt =
                passwordHistoryDomainService.findLastHistory(authUserId);

        boolean fromInitialOrReset = lastHistoryOpt
                .map(PasswordHistory::changeType)
                .map(type -> type == PasswordChangeType.INITIAL_REGISTER
                          || type == PasswordChangeType.ADMIN_RESET)
                .orElse(true); // 念のため履歴なしも「変更必須」とみなす

        LocalDateTime lastChangedAt = lastHistoryOpt
                .map(PasswordHistory::changedAt)
                .orElse(null);

        boolean passwordExpired =
                authPolicyDomainService.isPasswordExpired(lastChangedAt, now);

        boolean mustChangePassword = fromInitialOrReset || passwordExpired;

        return new LoginSuccessResult(
                authUserId.value(),
                user.loginId().value(),
                now,
                previousLoginAtOpt.orElse(null),
                passwordExpired,
                mustChangePassword
        );
    }
}
```

---

# 2. LockControlSharedUseCase の設計

## 2.1 役割

* Spring Security の **認証失敗時**に呼ばれるユースケース
* 仕様に対応した挙動：

    * 通常失敗 → FAIL 履歴、連続失敗回数を増やして **6回でロック**
    * ロック中のログイン → `AUTH_LOGIN_HISTORY.result = 'LOCKED'` を記録（失敗カウントには含めない）
* FailureHandler のために

    * 「ロックされたかどうか」「現在ロック中かどうか」「失敗回数」などを返す

## 2.2 インタフェース案

FailureHandler 側では、

* 例外が `LockedException` の場合 → 「ロック中ログイン」とみなして `onLockedUserTried` を呼ぶ
* それ以外の `BadCredentialsException` 等 → `onLoginFailure` を呼ぶ

という使い分けを想定します。

```java
package com.myou.ec.ecsite.application.auth.sharedservice.internal;

import com.myou.ec.ecsite.domain.auth.model.value.LoginId;

public interface LockControlSharedUseCase {

    /**
     * パスワード不一致など「通常のログイン失敗」時の処理。
     * （ロック中の場合は呼ばず、onLockedUserTried を呼ぶ前提）
     */
    LoginFailureHandleResult onLoginFailure(LoginId loginId);

    /**
     * ロック状態のユーザがログインを試みたときの処理。
     * 失敗カウントには含めず、LOCKED 履歴のみ記録する。
     */
    LoginFailureHandleResult onLockedUserTried(LoginId loginId);
}
```

戻り値 DTO（record）：

```java
package com.myou.ec.ecsite.application.auth.sharedservice.internal;

/**
 * ログイン失敗処理の結果。
 * FailureHandler でメッセージ切り替えなどに使える情報。
 */
public record LoginFailureHandleResult(
        boolean userFound,                // loginId に対応するユーザが存在したか
        Long authUserId,                  // ユーザが存在する場合のみセット
        int consecutiveFailureCount,      // 連続失敗回数（ロック中試行では 0）
        boolean lockedBefore,             // 処理前に既にロックされていたか
        boolean lockedNow                 // 今回の処理で新たにロックされたか
) {}
```

## 2.3 利用する DomainService / Repository

依存：

* `AuthUserRepository`
* `AuthPolicyDomainService`（最大失敗回数 6）
* `LoginHistoryDomainService`
* `AccountLockDomainService`
* `Clock`

## 2.4 onLoginFailure のフロー

1. `now = LocalDateTime.now(clock)`
2. `AuthUserRepository.findByLoginId(loginId)` でユーザ取得

    * 見つからない場合：

        * セキュリティ的には「存在しないユーザ」という情報を出したくないので
          → 履歴を残さず `userFound = false` で返却し、画面は汎用メッセージ表示
3. ユーザが存在する場合：

    * `AuthUserId userId = user.id()`
    * `boolean lockedBefore = accountLockDomainService.isLocked(userId)`
    * `lockedBefore` が true の場合：

        * 「ロック中試行」なので FAIL ではなく `onLockedUserTried` 相当の処理に回してもよいが、
          今回は呼び出し元で分ける前提なのでここでは **ロック中では呼ばれない**想定にするのがシンプルです。
    * `LoginHistoryDomainService.recordFailure(userId, now)` で FAIL 履歴登録
    * `int failCount = loginHistoryDomainService.countConsecutiveFailuresSinceLastSuccessOrUnlock(userId);`
    * `boolean lockedNow = (failCount >= authPolicyDomainService.getLoginFailMaxCount())`
    * `lockedNow` が true の場合：

        * `AccountLockDomainService.lock(userId, "LOGIN_FAIL_THRESHOLD", user.loginId())`
4. `LoginFailureHandleResult` を組み立てて返却

## 2.5 onLockedUserTried のフロー

1. `now = LocalDateTime.now(clock)`
2. `AuthUserRepository.findByLoginId(loginId)`

    * 見つからなければ `userFound = false` で返却
3. 見つかった場合：

    * `LoginHistoryDomainService.recordLockedAttempt(userId, now)` のみ呼ぶ
    * 失敗カウントには加算しない
    * `lockedBefore = true`, `lockedNow = true`, `consecutiveFailureCount = 0` として返却

## 2.6 実装イメージ

```java
package com.myou.ec.ecsite.application.auth.sharedservice.internal;

import com.myou.ec.ecsite.domain.auth.domainservice.AccountLockDomainService;
import com.myou.ec.ecsite.domain.auth.domainservice.AuthPolicyDomainService;
import com.myou.ec.ecsite.domain.auth.domainservice.LoginHistoryDomainService;
import com.myou.ec.ecsite.domain.auth.model.AuthUser;
import com.myou.ec.ecsite.domain.auth.model.value.AuthUserId;
import com.myou.ec.ecsite.domain.auth.model.value.LoginId;
import com.myou.ec.ecsite.domain.auth.repository.AuthUserRepository;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;

public class LockControlSharedUseCaseImpl implements LockControlSharedUseCase {

    private final AuthUserRepository authUserRepository;
    private final AuthPolicyDomainService authPolicyDomainService;
    private final LoginHistoryDomainService loginHistoryDomainService;
    private final AccountLockDomainService accountLockDomainService;
    private final Clock clock;

    public LockControlSharedUseCaseImpl(AuthUserRepository authUserRepository,
                                        AuthPolicyDomainService authPolicyDomainService,
                                        LoginHistoryDomainService loginHistoryDomainService,
                                        AccountLockDomainService accountLockDomainService,
                                        Clock clock) {
        this.authUserRepository = authUserRepository;
        this.authPolicyDomainService = authPolicyDomainService;
        this.loginHistoryDomainService = loginHistoryDomainService;
        this.accountLockDomainService = accountLockDomainService;
        this.clock = clock;
    }

    @Override
    public LoginFailureHandleResult onLoginFailure(LoginId loginId) {
        LocalDateTime now = LocalDateTime.now(clock);

        Optional<AuthUser> userOpt = authUserRepository.findByLoginId(loginId);
        if (userOpt.isEmpty()) {
            // ユーザが存在しない場合は履歴もロックも行わず、汎用エラー表示で対応
            return new LoginFailureHandleResult(false, null, 0, false, false);
        }

        AuthUser user = userOpt.get();
        AuthUserId authUserId = user.id();

        boolean lockedBefore = accountLockDomainService.isLocked(authUserId);
        if (lockedBefore) {
            // 本来ここには来ない前提（LockedException の場合は onLockedUserTried を呼ぶ）が、
            // 念のため LOCKED として履歴だけ残す。
            loginHistoryDomainService.recordLockedAttempt(authUserId, now);
            return new LoginFailureHandleResult(true, authUserId.value(), 0, true, true);
        }

        // 通常 FAIL
        loginHistoryDomainService.recordFailure(authUserId, now);

        int failCount =
                loginHistoryDomainService.countConsecutiveFailuresSinceLastSuccessOrUnlock(authUserId);

        boolean lockedNow = false;
        int maxFail = authPolicyDomainService.getLoginFailMaxCount();
        if (failCount >= maxFail) {
            // 閾値到達でロック
            accountLockDomainService.lock(authUserId, "LOGIN_FAIL_THRESHOLD", user.loginId());
            lockedNow = true;
        }

        return new LoginFailureHandleResult(true, authUserId.value(), failCount, lockedBefore, lockedNow);
    }

    @Override
    public LoginFailureHandleResult onLockedUserTried(LoginId loginId) {
        LocalDateTime now = LocalDateTime.now(clock);

        Optional<AuthUser> userOpt = authUserRepository.findByLoginId(loginId);
        if (userOpt.isEmpty()) {
            return new LoginFailureHandleResult(false, null, 0, false, false);
        }

        AuthUser user = userOpt.get();
        AuthUserId authUserId = user.id();

        // LOCKED 履歴のみ記録（失敗カウントには含めない）
        loginHistoryDomainService.recordLockedAttempt(authUserId, now);

        // すでにロック中であり、今回もロック状態である前提
        return new LoginFailureHandleResult(true, authUserId.value(), 0, true, true);
    }
}
```

---

# 3. Spring Security からの呼び出しイメージ（ざっくり）

## 3.1 SuccessHandler

```java
public class CustomAuthenticationSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    private final LoginSharedUseCase loginSharedUseCase;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        String loginIdValue = authentication.getName();
        LoginSuccessResult result =
                loginSharedUseCase.handleLoginSuccess(new LoginId(loginIdValue));

        // セッションに前回ログイン日時を保存（メニュー画面で表示用）
        request.getSession().setAttribute("previousLoginAt", result.previousLoginAt());

        if (result.mustChangePassword()) {
            getRedirectStrategy().sendRedirect(request, response, "/password/change");
        } else {
            getRedirectStrategy().sendRedirect(request, response, "/menu");
        }
    }
}
```

## 3.2 FailureHandler

```java
public class CustomAuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private final LockControlSharedUseCase lockControlSharedUseCase;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception)
            throws IOException, ServletException {

        String loginIdValue = request.getParameter("userId"); // usernameParameter に合わせる
        LoginFailureHandleResult result;

        if (exception instanceof LockedException) {
            result = lockControlSharedUseCase.onLockedUserTried(new LoginId(loginIdValue));
            // メッセージ: 「ロックされています。管理者に連絡してロック解除してください」
        } else {
            result = lockControlSharedUseCase.onLoginFailure(new LoginId(loginIdValue));
            // メッセージ: 
            //  - lockedNow == true なら「アカウントがロックされました」
            //  - それ以外は「ユーザIDまたはパスワードが正しくありません」
        }

        super.onAuthenticationFailure(request, response, exception);
    }
}
```

---

# 4. まとめ

* **LoginSharedUseCase**

    * 認証成功時に

        * 前回ログイン日時
        * パスワード期限切れ／初回・初期化直後かどうか
          をまとめて判定し、SuccessHandler が参照する DTO を返す。

* **LockControlSharedUseCase**

    * 認証失敗時に

        * FAIL / LOCKED 履歴
        * 連続失敗カウント
        * ロック判定（6回でロック）
          を統合的に扱い、FailureHandler がメッセージ切り替えに使える情報を返す。

この2つのユースケースを application 層に置くことで、
Spring Security まわりの設定クラス（presentation 層）からは **「ユースケースを呼ぶだけ」** のシンプルな形にできます。

次に進めるとしたら、

* `AuthAccountAdminSharedServiceImpl` / `PasswordChangeSharedServiceImpl` など
  **sharedService 実装クラス（業務Tが直接使う入口）の設計**
* もしくは **SecurityConfig のクラス設計（URL・ロール・Handler 設定）**

をやると、認証周りがほぼ一通り揃う状態になります。
