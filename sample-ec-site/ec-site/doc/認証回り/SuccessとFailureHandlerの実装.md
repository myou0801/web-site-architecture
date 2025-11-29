
* application層に「ログイン処理用 sharedService」
* presentation層に Success / FailureHandler（HTTP と URL だけ知っている）

という形で作り直します。

---

## 1. application層：ログイン処理用 sharedService

### 1-1. 成功／失敗の結果型

```java
package com.myou.ec.ecsite.application.auth.sharedservice;

/**
 * ログイン失敗の種類。
 */
public enum LoginFailureType {
    BAD_CREDENTIALS,
    LOCKED
}
```

```java
package com.myou.ec.ecsite.application.auth.sharedservice;

/**
 * ログイン成功時の結果。
 * 今は「パスワード変更が必須かどうか」だけを返す。
 */
public record LoginSuccessResult(
        boolean passwordChangeRequired
) {
}
```

### 1-2. インターフェース

```java
package com.myou.ec.ecsite.application.auth.sharedservice;

/**
 * ログイン成功/失敗時のドメイン処理（履歴記録・ロック制御など）を扱う sharedService。
 * presentation（Handler）から呼び出される。
 */
public interface LoginProcessSharedService {

    /**
     * ログイン成功時の処理。
     *
     * @param userIdValue   ユーザーID（認証成功済）
     * @param clientIp  クライアントIP
     * @param userAgent User-Agent
     * @return ログイン成功結果（パスワード変更強制が必要かどうか）
     */
    LoginSuccessResult onLoginSuccess(String userIdValue, String clientIp, String userAgent);

    /**
     * ログイン失敗時の処理。
     *
     * @param userIdValue   ユーザーID（フォーム入力値。存在しない場合もある）
     * @param clientIp  クライアントIP
     * @param userAgent User-Agent
     * @return 失敗種類（ロック or 認証エラー）
     */
    LoginFailureType onLoginFailure(String userIdValue, String clientIp, String userAgent);
}
```

### 1-3. 実装

```java
package com.myou.ec.ecsite.application.auth.sharedservice;

import com.myou.ec.ecsite.domain.auth.AuthDomainException;
import com.myou.ec.ecsite.domain.auth.model.AccountLockEvent;
import com.myou.ec.ecsite.domain.auth.model.AuthAccount;
import com.myou.ec.ecsite.domain.auth.model.LoginHistory;
import com.myou.ec.ecsite.domain.auth.model.PasswordHistory;
import com.myou.ec.ecsite.domain.auth.model.value.*;
import com.myou.ec.ecsite.domain.auth.repository.*;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class LoginProcessSharedServiceImpl implements LoginProcessSharedService {

    private final AuthAccountRepository authAccountRepository;
    private final AuthLoginHistoryRepository loginHistoryRepository;
    private final AuthPasswordHistoryRepository passwordHistoryRepository;
    private final AuthAccountLockHistoryRepository lockHistoryRepository;
    private final PasswordPolicy passwordPolicy;
    private final LockPolicy lockPolicy;

    public LoginProcessSharedServiceImpl(AuthAccountRepository authAccountRepository,
                                         AuthLoginHistoryRepository loginHistoryRepository,
                                         AuthPasswordHistoryRepository passwordHistoryRepository,
                                         AuthAccountLockHistoryRepository lockHistoryRepository,
                                         PasswordPolicy passwordPolicy,
                                         LockPolicy lockPolicy) {
        this.authAccountRepository = authAccountRepository;
        this.loginHistoryRepository = loginHistoryRepository;
        this.passwordHistoryRepository = passwordHistoryRepository;
        this.lockHistoryRepository = lockHistoryRepository;
        this.passwordPolicy = passwordPolicy;
        this.lockPolicy = lockPolicy;
    }

    @Override
    public LoginSuccessResult onLoginSuccess(String userIdValue, String clientIp, String userAgent) {
        UserId userId = new UserId(userIdValue);
        AuthAccount user = authAccountRepository.findByUserId(userId)
                .orElseThrow(() -> new AuthDomainException("ログイン成功後にアカウント情報が取得できません。"));

        AuthAccountId accountId = user.id();
        if (accountId == null) {
            throw new AuthDomainException("アカウントID未採番のためログイン履歴を記録できません。");
        }

        LocalDateTime now = LocalDateTime.now();

        // ログイン成功履歴を登録
        LoginHistory successHistory = LoginHistory.success(
                accountId,
                now,
                clientIp,
                userAgent,
                userId
        );
        loginHistoryRepository.save(successHistory);

        // パスワード変更が必要かどうか判定
        boolean mustChange = isPasswordChangeRequired(accountId, now);

        return new LoginSuccessResult(mustChange);
    }

    @Override
    public LoginFailureType onLoginFailure(String userIdValue, String clientIp, String userAgent) {
        if (userIdValue == null || userIdValue.isBlank()) {
            return LoginFailureType.BAD_CREDENTIALS;
        }

        UserId userId = new UserId(userIdValue);
        Optional<AuthAccount> optUser = authAccountRepository.findByUserId(userId);

        if (optUser.isEmpty()) {
            // アカウントが存在しない場合は履歴を残さない（情報漏洩防止）
            return LoginFailureType.BAD_CREDENTIALS;
        }

        AuthAccount user = optUser.get();
        AuthAccountId accountId = user.id();
        if (accountId == null) {
            return LoginFailureType.BAD_CREDENTIALS;
        }

        LocalDateTime now = LocalDateTime.now();

        // 現在ロック中か確認
        LockStatus status = lockHistoryRepository.getLockStatusByAccountId(accountId);
        if (status.isLocked()) {
            // ロック中のログインは LOCKED で履歴のみ（失敗カウントには含めない）
            LoginHistory lockedHistory = LoginHistory.locked(
                    accountId,
                    now,
                    clientIp,
                    userAgent,
                    userId
            );
            loginHistoryRepository.save(lockedHistory);
            return LoginFailureType.LOCKED;
        }

        // ロックされていない場合 → FAIL として履歴を追加
        LoginHistory failHistory = LoginHistory.fail(
                accountId,
                now,
                clientIp,
                userAgent,
                userId
        );
        loginHistoryRepository.save(failHistory);

        // 連続失敗回数をカウント（最後の SUCCESS or UNLOCK 以降）
        int consecutiveFails = loginHistoryRepository.countConsecutiveFailuresSinceLastSuccessOrUnlockByAccountId(accountId);

        if (lockPolicy.isOverThreshold(consecutiveFails)) {
            // 閾値超え → ロックイベント登録
            AccountLockEvent lockEvent = AccountLockEvent.lock(
                    accountId,
                    now,
                    "LOGIN_FAIL_THRESHOLD", // ロック理由
                    userId
            );
            lockHistoryRepository.save(lockEvent);
            return LoginFailureType.LOCKED;
        } else {
            return LoginFailureType.BAD_CREDENTIALS;
        }
    }

    /**
     * パスワード変更強制が必要かどうか判定する。
     * - 履歴なし → 強制（安全側）
     * - 履歴が INITIAL_REGISTER / ADMIN_RESET → 強制
     * - 履歴が USER_CHANGE で、有効期限切れ → 強制
     */
    private boolean isPasswordChangeRequired(AuthAccountId accountId, LocalDateTime now) {
        Optional<PasswordHistory> optLast = passwordHistoryRepository.findLastByAccountId(accountId);

        if (optLast.isEmpty()) {
            return true;
        }

        PasswordHistory last = optLast.get();
        if (last.changeType() == PasswordChangeType.INITIAL_REGISTER
                || last.changeType() == PasswordChangeType.ADMIN_RESET) {
            return true;
        }

        return passwordPolicy.isExpired(last.changedAt(), now);
    }
}```

---

## 2. presentation層：Success / Failure Handler

Handler は **presentation モジュール** に置く前提でパッケージを変えます。

```text
com.myou.ec.ecsite.presentation.auth.security
  ├─ AuthAuthenticationSuccessHandler
  └─ AuthAuthenticationFailureHandler
```

### 2-1. 認証成功ハンドラ

```java
package com.myou.ec.ecsite.presentation.auth.security;

import com.myou.ec.ecsite.application.auth.sharedservice.LoginProcessSharedService;
import com.myou.ec.ecsite.application.auth.sharedservice.LoginSuccessResult;
import com.myou.ec.ecsite.domain.auth.AuthDomainException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * ログイン成功時のハンドラ（presentation層）。
 * HTTP / URL のみを扱い、ドメイン処理は LoginProcessSharedService に委譲する。
 */
@Component
public class AuthAuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    public static final String SESSION_ATTR_PASSWORD_CHANGE_REQUIRED = "AUTH_PASSWORD_CHANGE_REQUIRED";

    private final LoginProcessSharedService loginProcessSharedService;

    // デフォルト遷移先URL（必要に応じて設定で差し替え可能にしてもよい）
    private final String defaultMenuUrl = "/menu";
    private final String passwordChangeUrl = "/password/change";

    public AuthAuthenticationSuccessHandler(LoginProcessSharedService loginProcessSharedService) {
        this.loginProcessSharedService = loginProcessSharedService;
        setDefaultTargetUrl(defaultMenuUrl);
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication)
            throws IOException, ServletException {

        String userId = extractUserId(authentication);
        String clientIp = request.getRemoteAddr();
        String userAgent = request.getHeader("User-Agent");

        // application 層に委譲してドメイン処理を実行
        LoginSuccessResult result =
                loginProcessSharedService.onLoginSuccess(userId, clientIp, userAgent);

        boolean mustChangePassword = result.passwordChangeRequired();
        String targetUrl = mustChangePassword ? passwordChangeUrl : defaultMenuUrl;

        if (mustChangePassword) {
            request.getSession(true)
                   .setAttribute(SESSION_ATTR_PASSWORD_CHANGE_REQUIRED, Boolean.TRUE);
        }

        clearAuthenticationAttributes(request);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    private String extractUserId(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof User userDetails) {
            return userDetails.getUsername();
        }
        if (principal instanceof String s) {
            return s;
        }
        throw new AuthDomainException("認証情報からユーザーIDを取得できません。");
    }
}
```

### 2-2. 認証失敗ハンドラ

```java
package com.myou.ec.ecsite.presentation.auth.security;

import com.myou.ec.ecsite.application.auth.sharedservice.LoginFailureType;
import com.myou.ec.ecsite.application.auth.sharedservice.LoginProcessSharedService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * ログイン失敗時のハンドラ（presentation層）。
 * HTTP / URL のみを扱い、ドメイン処理は LoginProcessSharedService に委譲する。
 */
@Component
public class AuthAuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    public static final String SESSION_ATTR_AUTH_ERROR_TYPE = "AUTH_ERROR_TYPE";
    public static final String ERROR_TYPE_LOCKED = "LOCKED";
    public static final String ERROR_TYPE_BAD_CREDENTIALS = "BAD_CREDENTIALS";

    private final LoginProcessSharedService loginProcessSharedService;

    // ログイン画面 URL
    private final String loginUrl = "/login?error";

    public AuthAuthenticationFailureHandler(LoginProcessSharedService loginProcessSharedService) {
        this.loginProcessSharedService = loginProcessSharedService;
        setDefaultFailureUrl(loginUrl);
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception)
            throws IOException, ServletException {

        // フォームの name="username" としている前提。違う場合はここを調整。
        String userId = request.getParameter("username");
        String clientIp = request.getRemoteAddr();
        String userAgent = request.getHeader("User-Agent");

        LoginFailureType type =
                loginProcessSharedService.onLoginFailure(userId, clientIp, userAgent);

        String errorTypeStr = switch (type) {
            case LOCKED -> ERROR_TYPE_LOCKED;
            case BAD_CREDENTIALS -> ERROR_TYPE_BAD_CREDENTIALS;
        };

        request.getSession(true)
               .setAttribute(SESSION_ATTR_AUTH_ERROR_TYPE, errorTypeStr);

        super.onAuthenticationFailure(request, response, exception);
    }
}
```

---

## 3. つなぎ込みイメージ（SecurityConfig）

あとは Security 設定で Handler を差し込むだけです（Boot なしでも同じ感じ）。

```java
http
    .formLogin(form -> form
        .loginPage("/login")
        .loginProcessingUrl("/login")
        .successHandler(authAuthenticationSuccessHandler)
        .failureHandler(authAuthenticationFailureHandler)
    );
```

* ログイン画面では `SESSION_ATTR_AUTH_ERROR_TYPE` を見て
  `"LOCKED"` ならロックメッセージ、`"BAD_CREDENTIALS"` なら認証エラーメッセージ。
* パスワード変更画面では `SESSION_ATTR_PASSWORD_CHANGE_REQUIRED` を見て、
  必要なら「有効期限切れ／初期パスワードです」などのメッセージを表示。

---

これで、

* **ドメイン／インフラ**：認証ルールと永続化
* **application（sharedService）**：ログイン成功/失敗時のユースケース処理
* **presentation（Handler）**：HTTP・セッション・URL のみ

というきれいなレイヤ分離になった形です。
もしフォームのパラメータ名や遷移URLをプロパティ化したくなったら、その部分だけ追加で調整していけます。
