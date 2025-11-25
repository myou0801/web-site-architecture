sharedService を **「業務Tが直接呼び出す入口」** として実装していきます。
パッケージは以下前提で書きます。

* `com.myou.ec.ecsite.application.auth.sharedservice`（AP基盤：application モジュール）

---

## README.md（application/auth/sharedservice 用）

# ecsite-auth SharedService

業務Tから利用する認証系の共通サービスです。  
DDD のドメインモデル（domain/auth）と Spring Security を組み合わせて、以下のユースケースを提供します。

## パッケージ構成

```text
com.myou.ec.ecsite.application.auth.sharedservice
 ├─ AuthUserContextSharedService
 ├─ AuthUserContextSharedServiceImpl
 ├─ PasswordChangeSharedService
 ├─ PasswordChangeSharedServiceImpl
 ├─ AuthAccountAdminSharedService
 └─ AuthAccountAdminSharedServiceImpl
```

## 役割

* `AuthUserContextSharedService`

    * **現在ログイン中のユーザ情報** や **前回ログイン日時**、ロール情報を取得する。
    * 業務画面で「前回ログイン日時」「メニューの制御」などに利用。

* `PasswordChangeSharedService`

    * ログイン中ユーザのパスワード変更処理。
    * パスワードポリシー（構文チェック／再利用禁止／有効期限）と履歴管理を行う。

* `AuthAccountAdminSharedService`

    * 管理者によるアカウント登録・有効/無効化・ロック解除・初期パスワードリセットなどを提供。
    * アカウント管理画面から利用。

## 依存

* domain/auth

    * `AuthUserRepository`, `AuthRoleRepository`, `AuthLoginHistoryRepository`,
      `AuthPasswordHistoryRepository`, `AuthAccountLockHistoryRepository`
    * `PasswordPolicy`, `LockPolicy` など
* Spring Security

    * `SecurityContextHolder`（現在ログイン中ユーザの loginId 取得）
    * `PasswordEncoder`（パスワードのハッシュ化／照合）



---

## AuthUserContextSharedService

### インタフェース

```java
package com.myou.ec.ecsite.application.auth.sharedservice;

import com.myou.ec.ecsite.domain.auth.model.AuthUser;
import com.myou.ec.ecsite.domain.auth.model.value.RoleCode;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 現在ログイン中のユーザ情報にアクセスする sharedService。
 */
public interface AuthUserContextSharedService {

    /**
     * 現在ログイン中の AuthUser を返す（存在しない場合は empty）。
     */
    Optional<AuthUser> findCurrentUser();

    /**
     * 現在ログイン中の AuthUser を返す（存在しない場合は例外）。
     */
    AuthUser getCurrentUserOrThrow();

    /**
     * 現在ログイン中ユーザの前回ログイン日時（直近 SUCCESS）の値を返す。
     * ログイン履歴が不足している場合は empty。
     */
    Optional<LocalDateTime> findPreviousLoginAt();

    /**
     * 現在ログイン中ユーザのロール一覧（RoleCode）を返す。
     */
    List<RoleCode> getCurrentUserRoles();

    /**
     * 指定ロールを保持しているか（Spring Security の権限ではなく、DB上のロール）。
     */
    boolean hasRole(RoleCode roleCode);
}
````

### 実装

```java
package com.myou.ec.ecsite.application.auth.sharedservice;

import com.myou.ec.ecsite.domain.auth.exception.AuthDomainException;
import com.myou.ec.ecsite.domain.auth.model.AuthUser;
import com.myou.ec.ecsite.domain.auth.model.value.AuthUserId;
import com.myou.ec.ecsite.domain.auth.model.value.LoginId;
import com.myou.ec.ecsite.domain.auth.model.value.RoleCode;
import com.myou.ec.ecsite.domain.auth.repository.AuthLoginHistoryRepository;
import com.myou.ec.ecsite.domain.auth.repository.AuthUserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AuthUserContextSharedServiceImpl implements AuthUserContextSharedService {

    private final AuthUserRepository authUserRepository;
    private final AuthLoginHistoryRepository loginHistoryRepository;

    public AuthUserContextSharedServiceImpl(AuthUserRepository authUserRepository,
                                            AuthLoginHistoryRepository loginHistoryRepository) {
        this.authUserRepository = authUserRepository;
        this.loginHistoryRepository = loginHistoryRepository;
    }

    @Override
    public Optional<AuthUser> findCurrentUser() {
        String loginId = getCurrentLoginIdFromSecurityContext();
        if (loginId == null) {
            return Optional.empty();
        }
        return authUserRepository.findByLoginId(new LoginId(loginId));
    }

    @Override
    public AuthUser getCurrentUserOrThrow() {
        return findCurrentUser()
                .orElseThrow(() -> new AuthDomainException("ログインユーザ情報が取得できません。"));
    }

    @Override
    public Optional<LocalDateTime> findPreviousLoginAt() {
        return findCurrentUser()
                .map(AuthUser::id)
                .flatMap(userId -> loginHistoryRepository.findPreviousSuccessLoginAt(userId));
    }

    @Override
    public List<RoleCode> getCurrentUserRoles() {
        return getCurrentUserOrThrow().roleCodes();
    }

    @Override
    public boolean hasRole(RoleCode roleCode) {
        return getCurrentUserOrThrow()
                .roleCodes()
                .stream()
                .anyMatch(rc -> rc.value().equals(roleCode.value()));
    }

    private String getCurrentLoginIdFromSecurityContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        // シンプルに username=loginId として扱う方針
        if (principal instanceof org.springframework.security.core.userdetails.User userDetails) {
            return userDetails.getUsername();
        }
        if (principal instanceof String s) {
            return s;
        }
        return null;
    }
}
```

---

## PasswordChangeSharedService

### インタフェース

```java
package com.myou.ec.ecsite.application.auth.sharedservice;

/**
 * ログイン中ユーザのパスワード変更を行う sharedService。
 */
public interface PasswordChangeSharedService {

    /**
     * ログイン中ユーザのパスワードを変更する。
     *
     * @param currentRawPassword 現在のパスワード（平文）
     * @param newRawPassword     新しいパスワード（平文）
     */
    void changePasswordOfCurrentUser(String currentRawPassword, String newRawPassword);
}
```

### 実装

```java
package com.myou.ec.ecsite.application.auth.sharedservice;

import com.myou.ec.ecsite.domain.auth.exception.AuthDomainException;
import com.myou.ec.ecsite.domain.auth.exception.PasswordPolicyViolationException;
import com.myou.ec.ecsite.domain.auth.exception.PasswordReuseNotAllowedException;
import com.myou.ec.ecsite.domain.auth.model.AuthUser;
import com.myou.ec.ecsite.domain.auth.model.PasswordHistory;
import com.myou.ec.ecsite.domain.auth.model.value.AuthUserId;
import com.myou.ec.ecsite.domain.auth.model.value.EncodedPassword;
import com.myou.ec.ecsite.domain.auth.model.value.LoginId;
import com.myou.ec.ecsite.domain.auth.model.value.PasswordPolicy;
import com.myou.ec.ecsite.domain.auth.repository.AuthPasswordHistoryRepository;
import com.myou.ec.ecsite.domain.auth.repository.AuthUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PasswordChangeSharedServiceImpl implements PasswordChangeSharedService {

    private final AuthUserContextSharedService userContextSharedService;
    private final AuthUserRepository authUserRepository;
    private final AuthPasswordHistoryRepository passwordHistoryRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicy passwordPolicy;

    public PasswordChangeSharedServiceImpl(AuthUserContextSharedService userContextSharedService,
                                           AuthUserRepository authUserRepository,
                                           AuthPasswordHistoryRepository passwordHistoryRepository,
                                           PasswordEncoder passwordEncoder,
                                           PasswordPolicy passwordPolicy) {
        this.userContextSharedService = userContextSharedService;
        this.authUserRepository = authUserRepository;
        this.passwordHistoryRepository = passwordHistoryRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicy = passwordPolicy;
    }

    @Override
    public void changePasswordOfCurrentUser(String currentRawPassword, String newRawPassword) {
        AuthUser user = userContextSharedService.getCurrentUserOrThrow();
        AuthUserId userId = user.id();
        if (userId == null) {
            throw new AuthDomainException("ユーザID未採番のためパスワード変更ができません。");
        }

        // 現在のパスワード検証
        if (!passwordEncoder.matches(currentRawPassword, user.encodedPassword().value())) {
            throw new PasswordPolicyViolationException("現在のパスワードが正しくありません。");
        }

        // パスワード構文チェック
        passwordPolicy.validateSyntax(newRawPassword, user.loginId());

        // 履歴による再利用禁止チェック（直近 N 件）
        List<PasswordHistory> recentHistories =
                passwordHistoryRepository.findRecentByUserId(userId, passwordPolicy.historyGenerationCount());

        for (PasswordHistory history : recentHistories) {
            if (passwordEncoder.matches(newRawPassword, history.encodedPassword().value())) {
                throw new PasswordReuseNotAllowedException();
            }
        }

        // 新しいパスワードをハッシュ化
        String encoded = passwordEncoder.encode(newRawPassword);
        EncodedPassword encodedPassword = new EncodedPassword(encoded);

        // ユーザのパスワード更新
        user.changePassword(encodedPassword);
        authUserRepository.save(user);

        // パスワード履歴登録
        LocalDateTime now = LocalDateTime.now();
        LoginId operator = user.loginId(); // 自分自身が変更
        PasswordHistory history = PasswordHistory.userChange(userId, encodedPassword, now, operator);
        passwordHistoryRepository.save(history);
    }
}
```

---

## AuthAccountAdminSharedService

### インタフェース

```java
package com.myou.ec.ecsite.application.auth.sharedservice;

import com.myou.ec.ecsite.domain.auth.model.AuthUser;
import com.myou.ec.ecsite.domain.auth.model.value.AuthUserId;
import com.myou.ec.ecsite.domain.auth.model.value.LoginId;
import com.myou.ec.ecsite.domain.auth.model.value.RoleCode;

import java.util.List;

/**
 * 管理者によるアカウント管理の sharedService。
 */
public interface AuthAccountAdminSharedService {

    /**
     * アカウントを新規登録する。
     *
     * @param loginId   ログインID
     * @param rawPassword 初期パスワード（平文）
     * @param roleCodes 付与するロール一覧
     * @param operator  操作ユーザ（管理者）の loginId
     * @return 登録された AuthUser
     */
    AuthUser registerAccount(LoginId loginId,
                             String rawPassword,
                             List<RoleCode> roleCodes,
                             LoginId operator);

    /**
     * 初期パスワードにリセットし、ロックも解除する。
     *
     * @param targetUserId 対象ユーザID
     * @param operator     操作ユーザ（管理者）の loginId
     */
    void resetPasswordToInitial(AuthUserId targetUserId, LoginId operator);

    /**
     * アカウントロックを解除する。
     */
    void unlockAccount(AuthUserId targetUserId, LoginId operator);

    /**
     * アカウントを無効化する。
     */
    void disableAccount(AuthUserId targetUserId, LoginId operator);

    /**
     * アカウントを有効化する。
     */
    void enableAccount(AuthUserId targetUserId, LoginId operator);
}
```

### 実装

```java
package com.myou.ec.ecsite.application.auth.sharedservice;

import com.myou.ec.ecsite.domain.auth.exception.AuthDomainException;
import com.myou.ec.ecsite.domain.auth.model.AccountLockEvent;
import com.myou.ec.ecsite.domain.auth.model.AuthUser;
import com.myou.ec.ecsite.domain.auth.model.PasswordHistory;
import com.myou.ec.ecsite.domain.auth.model.value.AuthUserId;
import com.myou.ec.ecsite.domain.auth.model.value.EncodedPassword;
import com.myou.ec.ecsite.domain.auth.model.value.LoginId;
import com.myou.ec.ecsite.domain.auth.model.value.RoleCode;
import com.myou.ec.ecsite.domain.auth.model.value.PasswordPolicy;
import com.myou.ec.ecsite.domain.auth.repository.AuthAccountLockHistoryRepository;
import com.myou.ec.ecsite.domain.auth.repository.AuthPasswordHistoryRepository;
import com.myou.ec.ecsite.domain.auth.repository.AuthRoleRepository;
import com.myou.ec.ecsite.domain.auth.repository.AuthUserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuthAccountAdminSharedServiceImpl implements AuthAccountAdminSharedService {

    private final AuthUserRepository authUserRepository;
    private final AuthRoleRepository authRoleRepository;
    private final AuthPasswordHistoryRepository passwordHistoryRepository;
    private final AuthAccountLockHistoryRepository lockHistoryRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicy passwordPolicy;
    private final String initialPassword;

    public AuthAccountAdminSharedServiceImpl(AuthUserRepository authUserRepository,
                                             AuthRoleRepository authRoleRepository,
                                             AuthPasswordHistoryRepository passwordHistoryRepository,
                                             AuthAccountLockHistoryRepository lockHistoryRepository,
                                             PasswordEncoder passwordEncoder,
                                             PasswordPolicy passwordPolicy,
                                             @Value("${auth.initial-password:password123}") String initialPassword) {
        this.authUserRepository = authUserRepository;
        this.authRoleRepository = authRoleRepository;
        this.passwordHistoryRepository = passwordHistoryRepository;
        this.lockHistoryRepository = lockHistoryRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicy = passwordPolicy;
        this.initialPassword = initialPassword;
    }

    @Override
    public AuthUser registerAccount(LoginId loginId,
                                    String rawPassword,
                                    List<RoleCode> roleCodes,
                                    LoginId operator) {

        LocalDateTime now = LocalDateTime.now();

        // パスワードポリシー（構文）チェック
        passwordPolicy.validateSyntax(rawPassword, loginId);

        // パスワードハッシュ化
        EncodedPassword encodedPassword = new EncodedPassword(passwordEncoder.encode(rawPassword));

        // AuthUser 作成 & 保存
        AuthUser user = AuthUser.newUser(loginId, encodedPassword, roleCodes, now, operator);
        authUserRepository.save(user);

        // ID 採番後のユーザを再取得（ID 必要なため）
        AuthUser savedUser = authUserRepository.findByLoginId(loginId)
                .orElseThrow(() -> new AuthDomainException("アカウント登録後の再取得に失敗しました。"));

        AuthUserId userId = savedUser.id();
        if (userId == null) {
            throw new AuthDomainException("採番されたユーザIDが取得できません。");
        }

        // ユーザロール設定
        authRoleRepository.saveUserRoles(userId, roleCodes);

        // パスワード履歴登録（初回登録）
        PasswordHistory history = PasswordHistory.initialRegister(
                userId,
                encodedPassword,
                now,
                operator
        );
        passwordHistoryRepository.save(history);

        return savedUser;
    }

    @Override
    public void resetPasswordToInitial(AuthUserId targetUserId, LoginId operator) {
        AuthUser user = authUserRepository.findById(targetUserId)
                .orElseThrow(() -> new AuthDomainException("対象アカウントが存在しません。"));

        LocalDateTime now = LocalDateTime.now();

        // 初期パスワードの構文チェック（ポリシーに沿っている前提だが、一応検証）
        passwordPolicy.validateSyntax(initialPassword, user.loginId());

        // ハッシュ化
        EncodedPassword encodedPassword = new EncodedPassword(passwordEncoder.encode(initialPassword));

        // パスワード更新
        user.changePassword(encodedPassword);
        authUserRepository.save(user);

        // パスワード履歴（ADMIN_RESET）
        PasswordHistory history = PasswordHistory.adminReset(
                targetUserId,
                encodedPassword,
                now,
                operator
        );
        passwordHistoryRepository.save(history);

        // ロック解除イベント（パスワード初期化時はロック解除も行う）
        AccountLockEvent unlockEvent = AccountLockEvent.unlock(
                targetUserId,
                now,
                "ADMIN_RESET_AND_UNLOCK",
                operator
        );
        lockHistoryRepository.save(unlockEvent);
    }

    @Override
    public void unlockAccount(AuthUserId targetUserId, LoginId operator) {
        AuthUser user = authUserRepository.findById(targetUserId)
                .orElseThrow(() -> new AuthDomainException("対象アカウントが存在しません。"));

        LocalDateTime now = LocalDateTime.now();

        AccountLockEvent unlockEvent = AccountLockEvent.unlock(
                user.id(),
                now,
                "ADMIN_UNLOCK",
                operator
        );
        lockHistoryRepository.save(unlockEvent);
    }

    @Override
    public void disableAccount(AuthUserId targetUserId, LoginId operator) {
        AuthUser user = authUserRepository.findById(targetUserId)
                .orElseThrow(() -> new AuthDomainException("対象アカウントが存在しません。"));

        user.disable();
        authUserRepository.save(user);
    }

    @Override
    public void enableAccount(AuthUserId targetUserId, LoginId operator) {
        AuthUser user = authUserRepository.findById(targetUserId)
                .orElseThrow(() -> new AuthDomainException("対象アカウントが存在しません。"));

        user.enable();
        authUserRepository.save(user);
    }
}
```

---

これで、AP基盤側の **sharedService 3種の実装** が揃いました。

* 業務Tの Service クラスからは：

    * ログイン中情報 → `AuthUserContextSharedService`
    * パスワード変更画面 → `PasswordChangeSharedService`
    * アカウント管理画面 → `AuthAccountAdminSharedService`
      を DI して呼び出すイメージです。

このあとやるとしたら：

* Spring Security の Success/FailureHandler から

    * `AuthLoginHistoryRepository` / `AuthAccountLockHistoryRepository` を使って履歴・ロック制御
* ログイン画面／メニュー画面の Controller 実装（前回ログイン日時の表示など）

あたりですね。必要になったらそこも一緒に詰めていきましょう。
