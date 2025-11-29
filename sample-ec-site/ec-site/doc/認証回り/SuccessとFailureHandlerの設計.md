Success / FailureHandler は「Spring Security ⇔ AP基盤ユースケース」をつなぐ最後のピースなので、
**どの情報を受け取って、どこに書き込んで、どこへリダイレクトするか**を中心に設計していきます。

---

## 1. 前提・全体像

* バージョン

    * Java 25
    * Spring Security 6.5（`SecurityFilterChain` 方式）
* AP基盤側ユースケース

    * `LoginSharedUseCase` … 認証成功時の処理
    * `LockControlSharedUseCase` … 認証失敗時／ロック時の処理
* 画面側

    * ログイン画面：`/login`
    * メニュー画面：`/menu`
    * パスワード変更画面：`/password/change`
* メッセージ仕様

    * ロック中：
      「ロックされています。管理者に連絡してロック解除してください」
    * 期限切れパスワード → 認証成功後にパスワード変更画面へ遷移し、
      「パスワードの有効期限が切れています。変更してください。」 を表示
    * 初回/初期化直後 → 認証成功後にパスワード変更画面へ遷移（メッセージはお好みで）

---

## 2. LoginSuccessResult の少しだけ拡張

ハンドラでメッセージ種別を切り替えたいので、
**「なぜパスワード変更が必要なのか」** も分かるようにしておきます。

```java
package com.myou.ec.ecsite.application.auth.sharedservice.internal;

import java.time.LocalDateTime;

public enum PasswordChangeReason {
    NONE,               // 変更不要
    INITIAL_OR_RESET,   // 初回登録 or 管理者初期化後
    EXPIRED             // 有効期限切れ
}

/**
 * 認証成功後のフロー判定結果。
 */
public record LoginSuccessResult(
        long authAccountId,
        String userId,
        LocalDateTime loginAt,
        LocalDateTime previousLoginAt,   // null = 前回なし
        boolean passwordExpired,
        boolean mustChangePassword,
        PasswordChangeReason passwordChangeReason
) {}
```

`LoginSharedUseCaseImpl` 側で：

* `fromInitialOrReset` → `PasswordChangeReason.INITIAL_OR_RESET`
* `passwordExpired` → `PasswordChangeReason.EXPIRED`
* 両方 false → `PasswordChangeReason.NONE`

をセットするイメージです。

---

## 3. AuthenticationSuccessHandler の設計

### 3.1 クラスの責務

* 認証成功時に `LoginSharedUseCase` を呼ぶ
* セッションに以下をセット

    * `previousLoginAt` … メニュー画面での前回ログイン日時表示用
    * `passwordChangeReason` … パスワード変更画面のメッセージ切り替え用
* 遷移先を判断

    * `mustChangePassword == true` → `/password/change`
    * それ以外 → `/menu`（または SavedRequest）

### 3.2 設計（クラス定義）

```java
package com.myou.ec.ecsite.presentation.auth.security;

import com.myou.ec.ecsite.application.auth.sharedservice.internal.LoginSharedUseCase;
import com.myou.ec.ecsite.application.auth.sharedservice.internal.LoginSuccessResult;
import com.myou.ec.ecsite.application.auth.sharedservice.internal.PasswordChangeReason;
import com.myou.ec.ecsite.domain.auth.model.value.UserId;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;

import java.io.IOException;

/**
 * 認証成功時ハンドラ。
 * - LoginSharedUseCase を呼び出し
 * - 前回ログイン日時をセッションへ保存
 * - パスワード変更が必要なら変更画面へリダイレクト
 */
public class CustomAuthenticationSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    public static final String SESSION_KEY_PREVIOUS_LOGIN_AT = "previousLoginAt";
    public static final String SESSION_KEY_PASSWORD_CHANGE_REASON = "passwordChangeReason";

    private final LoginSharedUseCase loginSharedUseCase;
    private final String menuUrl;
    private final String passwordChangeUrl;

    public CustomAuthenticationSuccessHandler(LoginSharedUseCase loginSharedUseCase) {
        this(loginSharedUseCase, "/menu", "/password/change");
    }

    public CustomAuthenticationSuccessHandler(LoginSharedUseCase loginSharedUseCase,
                                              String menuUrl,
                                              String passwordChangeUrl) {
        this.loginSharedUseCase = loginSharedUseCase;
        this.menuUrl = menuUrl;
        this.passwordChangeUrl = passwordChangeUrl;
        // SavedRequestAwareAuthenticationSuccessHandler の defaultTargetUrl も念のためセットしておく
        setDefaultTargetUrl(menuUrl);
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication)
            throws IOException, ServletException {

        String userIdValue = authentication.getName();   // usernameParameter と一致
        LoginSuccessResult result =
                loginSharedUseCase.handleLoginSuccess(new UserId(userIdValue));

        HttpSession session = request.getSession(true);
        session.setAttribute(SESSION_KEY_PREVIOUS_LOGIN_AT, result.previousLoginAt());

        if (result.mustChangePassword()) {
            // パスワード変更理由をセッションへ
            session.setAttribute(SESSION_KEY_PASSWORD_CHANGE_REASON, result.passwordChangeReason());
            // 強制的にパスワード変更画面へリダイレクト（SavedRequest は無視）
            getRedirectStrategy().sendRedirect(request, response, passwordChangeUrl);
            return;
        }

        // 通常は SavedRequest があればそこへ、それ以外は menuUrl
        super.onAuthenticationSuccess(request, response, authentication);
    }
}
```

### 3.3 パスワード変更画面側での利用

パスワード変更画面の Controller では、以下のようにしてメッセージを出し分けできます。

```java
PasswordChangeReason reason =
    (PasswordChangeReason) session.getAttribute(SESSION_KEY_PASSWORD_CHANGE_REASON);

if (reason == PasswordChangeReason.EXPIRED) {
    model.addAttribute("message", "パスワードの有効期限が切れています。変更してください。");
} else if (reason == PasswordChangeReason.INITIAL_OR_RESET) {
    model.addAttribute("message", "初回ログインまたはパスワード初期化後のため、パスワードを変更してください。");
}
```

---

## 4. AuthenticationFailureHandler の設計

### 4.1 クラスの責務

* 認証失敗時に `LockControlSharedUseCase` を呼び出し、

    * 連続失敗回数
    * ロックされたかどうか
* を判定する
* ロック関連メッセージ/汎用メッセージをリクエスト／セッションに格納する
* ログイン画面へリダイレクト（`/login?error` など）

### 4.2 メッセージ方針

* ロック中（すでにロック / 今回ロックされた）：

    * `result.lockedNow()` または `exception instanceof LockedException`
    * → 「ロックされています。管理者に連絡してロック解除してください」
* それ以外の失敗：

    * → 「ユーザIDまたはパスワードが正しくありません」（例）
* メッセージは i18n を考えてコードで持つのもアリ：

    * `auth.error.locked`
    * `auth.error.badCredentials`
    * など

ここではシンプルに「メッセージ文字列」をリクエスト属性 `loginErrorMessage` に載せる設計にします。

### 4.3 クラス定義

```java
package com.myou.ec.ecsite.presentation.auth.security;

import com.myou.ec.ecsite.application.auth.sharedservice.internal.LockControlSharedUseCase;
import com.myou.ec.ecsite.application.auth.sharedservice.internal.LoginFailureHandleResult;
import com.myou.ec.ecsite.domain.auth.model.value.UserId;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;

import java.io.IOException;

/**
 * 認証失敗時ハンドラ。
 * - LockControlSharedUseCase を呼び出して履歴・ロック判定を行う
 * - ログインエラーメッセージをリクエスト属性にセットして /login?error へリダイレクト
 */
public class CustomAuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    public static final String REQUEST_KEY_LOGIN_ERROR_MESSAGE = "loginErrorMessage";

    private final LockControlSharedUseCase lockControlSharedUseCase;
    private final String usernameParameter;

    public CustomAuthenticationFailureHandler(LockControlSharedUseCase lockControlSharedUseCase) {
        this(lockControlSharedUseCase, "userId", "/login?error");
    }

    public CustomAuthenticationFailureHandler(LockControlSharedUseCase lockControlSharedUseCase,
                                              String usernameParameter,
                                              String failureUrl) {
        this.lockControlSharedUseCase = lockControlSharedUseCase;
        this.usernameParameter = usernameParameter;
        setDefaultFailureUrl(failureUrl); // /login?error
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception)
            throws IOException, ServletException {

        String userIdValue = request.getParameter(usernameParameter);
        LoginFailureHandleResult result;

        if (exception instanceof LockedException) {
            // Spring Security が既にロックだと判断したケース
            result = lockControlSharedUseCase.onLockedUserTried(new UserId(userIdValue));
        } else {
            result = lockControlSharedUseCase.onLoginFailure(new UserId(userIdValue));
        }

        // メッセージ決定
        String message;
        if (result.lockedNow() || exception instanceof LockedException) {
            // 仕様：ロック中のメッセージ
            message = "ロックされています。管理者に連絡してロック解除してください。";
        } else {
            // ロックしていない通常の失敗
            message = "ユーザーIDまたはパスワードが正しくありません。";
        }

        // リクエスト属性にセット（リダイレクト後にも使いたければセッションに載せる）
        request.getSession().setAttribute(REQUEST_KEY_LOGIN_ERROR_MESSAGE, message);

        // 親クラスに任せて /login?error へリダイレクト
        super.onAuthenticationFailure(request, response, exception);
    }
}```

### 4.4 ログイン画面側での利用

* ログイン画面の Controller（GET `/login`）で、セッションから取り出して Model に詰める：

```java
String errorMessage = (String) session.getAttribute(REQUEST_KEY_LOGIN_ERROR_MESSAGE);
if (errorMessage != null) {
    model.addAttribute("loginErrorMessage", errorMessage);
    session.removeAttribute(REQUEST_KEY_LOGIN_ERROR_MESSAGE);
}
```

* Thymeleaf 側では：

```html
<div th:if="${loginErrorMessage}" th:text="${loginErrorMessage}" class="error"></div>
```

---

## 5. SecurityConfig からの組み込み（ざっくり）

最後に、SecurityConfig のイメージだけざっと書いておきます。
（Spring Boot なしのプレーン Spring Security 6.5 前提）

```java
package com.myou.ec.ecsite.presentation.auth.security;

import com.myou.ec.ecsite.application.auth.sharedservice.internal.LoginSharedUseCase;
import com.myou.ec.ecsite.application.auth.sharedservice.internal.LockControlSharedUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   LoginSharedUseCase loginSharedUseCase,
                                                   LockControlSharedUseCase lockControlSharedUseCase) throws Exception {

        CustomAuthenticationSuccessHandler successHandler =
                new CustomAuthenticationSuccessHandler(loginSharedUseCase);

        CustomAuthenticationFailureHandler failureHandler =
                new CustomAuthenticationFailureHandler(lockControlSharedUseCase, "userId", "/login?error");

        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/css/**", "/js/**", "/images/**").permitAll()
                .requestMatchers("/account/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .usernameParameter("userId")
                .passwordParameter("password")
                .successHandler(successHandler)
                .failureHandler(failureHandler)
                .permitAll()
            )
            .logout(Customizer.withDefaults());

        return http.build();
    }
}
```

---

## 6. まとめ

* **SuccessHandler**

    * `LoginSharedUseCase` を呼び出して

        * 前回ログイン日時をセッションへ
        * パスワード変更が必要か（理由付き）を判定
    * `mustChangePassword` が true の場合は `/password/change` へ強制リダイレクト

* **FailureHandler**

    * `LockControlSharedUseCase` を呼び出して

        * FAIL / LOCKED 履歴
        * 連続失敗回数
        * ロック有無
          を更新
    * ロック中／ロックされた場合はロックメッセージ、それ以外は汎用エラーメッセージをセットし `/login?error` へリダイレクト

この設計で、
Spring Security のイベント → DDDベースのドメインロジック → 画面表示メッセージ／遷移
までが一貫した形で繋がるようになります。
