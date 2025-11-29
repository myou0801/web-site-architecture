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
 ├─ AuthAccountContextSharedService
 ├─ AuthAccountContextSharedServiceImpl
 ├─ PasswordChangeSharedService
 ├─ PasswordChangeSharedServiceImpl
 ├─ AuthAccountAdminSharedService
 └─ AuthAccountAdminSharedServiceImpl
```

<h2>役割</h2>

* `AuthAccountContextSharedService`

    * **現在ログイン中のアカウント情報** や **前回ログイン日時**、ロール情報を取得する。
    * 業務画面で「前回ログイン日時」「メニューの制御」などに利用。

* `PasswordChangeSharedService`

    * ログイン中アカウントのパスワード変更処理。
    * パスワードポリシー（構文チェック／再利用禁止／有効期限）と履歴管理を行う。

* `AuthAccountAdminSharedService`

    * 管理者によるアカウント登録・有効/無効化・ロック解除・初期パスワードリセットなどを提供。
    * アカウント管理画面から利用。

<h2>依存</h2>

* domain/auth

    * `AuthAccountRepository`, `AuthRoleRepository`, `AuthLoginHistoryRepository`,
      `AuthPasswordHistoryRepository`, `AuthAccountLockHistoryRepository`
    * `PasswordPolicy`, `LockPolicy` など
* Spring Security

    * `SecurityContextHolder`（現在ログイン中アカウントの userId 取得）
    * `PasswordEncoder`（パスワードのハッシュ化／照合）



---

<h2>AuthAccountContextSharedService</h2>

<h3>インタフェース</h3>

```java
package com.myou.ec.ecsite.application.auth.sharedservice;

import com.myou.ec.ecsite.domain.auth.model.AuthAccount;
import com.myou.ec.ecsite.domain.auth.model.value.RoleCode;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 現在ログイン中のアカウント情報にアクセスする sharedService。
 */
public interface AuthAccountContextSharedService {

    /**
     * 現在ログイン中の AuthAccount を返す（存在しない場合は empty）。
     */
    Optional<AuthAccount> findCurrentUser();

    /**
     * 現在ログイン中の AuthAccount を返す（存在しない場合は例外）。
     */
    AuthAccount getCurrentUserOrThrow();

    /**
     * 現在ログイン中アカウントの前回ログイン日時（直近 SUCCESS）の値を返す。
     * ログイン履歴が不足している場合は empty。
     */
    Optional<LocalDateTime> findPreviousLoginAt();

    /**
     * 現在ログイン中アカウントのロール一覧（RoleCode）を返す。
     */
    List<RoleCode> getCurrentUserRoles();

    /**
     * 指定ロールを保持しているか（Spring Security の権限ではなく、DB上のロール）。
     */
    boolean hasRole(RoleCode roleCode);
}
````

<h3>実装</h3>

```java
package com.myou.ec.ecsite.application.auth.sharedservice;

import com.myou.ec.ecsite.domain.auth.exception.AuthDomainException;
import com.myou.ec.ecsite.domain.auth.model.AuthAccount;
import com.myou.ec.ecsite.domain.auth.model.LoginHistory;
import com.myou.ec.ecsite.domain.auth.model.value.UserId;
import com.myou.ec.ecsite.domain.auth.model.value.RoleCode;
import com.myou.ec.ecsite.domain.auth.repository.AuthLoginHistoryRepository;
import com.myou.ec.ecsite.domain.auth.repository.AuthAccountRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AuthAccountContextSharedServiceImpl implements AuthAccountContextSharedService {

    private final AuthAccountRepository authAccountRepository;
    private final AuthLoginHistoryRepository loginHistoryRepository;

    public AuthAccountContextSharedServiceImpl(AuthAccountRepository authAccountRepository,
                                            AuthLoginHistoryRepository loginHistoryRepository) {
        this.authAccountRepository = authAccountRepository;
        this.loginHistoryRepository = loginHistoryRepository;
    }

    @Override
    public Optional<AuthAccount> findCurrentUser() {
        String userId = getCurrentUserIdFromSecurityContext();
        if (userId == null) {
            return Optional.empty();
        }
        return authAccountRepository.findByUserId(new UserId(userId));
    }

    @Override
    public AuthAccount getCurrentUserOrThrow() {
        return findCurrentUser()
                .orElseThrow(() -> new AuthDomainException("ログインアカウント情報が取得できません。"));
    }

    @Override
    public Optional<LocalDateTime> findPreviousLoginAt() {
        return findCurrentUser()
                .map(AuthAccount::id)
                .flatMap(accountId -> loginHistoryRepository.findPreviousSuccessLoginAtByAccountId(accountId))
                .map(LoginHistory::loginAt);
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

    private String getCurrentUserIdFromSecurityContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        // シンプルに username=userId として扱う方針
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

<h2>PasswordChangeSharedService</h2>

<h3>インタフェース</h3>

```java
package com.myou.ec.ecsite.application.auth.sharedservice;

/**
 * ログイン中アカウントのパスワード変更を行う sharedService。
 */
public interface PasswordChangeSharedService {

    /**
     * ログイン中アカウントのパスワードを変更する。
     *
     * @param currentRawPassword 現在のパスワード（平文）
     * @param newRawPassword     新しいパスワード（平文）
     */
    void changePasswordOfCurrentUser(String currentRawPassword, String newRawPassword);
}
```

<h3>実装</h3>

```java
package com.myou.ec.ecsite.application.auth.sharedservice;

import com.myou.ec.ecsite.domain.auth.exception.AuthDomainException;
import com.myou.ec.ecsite.domain.auth.exception.PasswordPolicyViolationException;
import com.myou.ec.ecsite.domain.auth.exception.PasswordReuseNotAllowedException;
import com.myou.ec.ecsite.domain.auth.model.AuthAccount;
import com.myou.ec.ecsite.domain.auth.model.PasswordHistory;
import com.myou.ec.ecsite.domain.auth.model.value.AuthAccountId;
import com.myou.ec.ecsite.domain.auth.model.value.EncodedPassword;
import com.myou.ec.ecsite.domain.auth.model.value.UserId;
import com.myou.ec.ecsite.domain.auth.model.policy.PasswordPolicy;
import com.myou.ec.ecsite.domain.auth.repository.AuthPasswordHistoryRepository;
import com.myou.ec.ecsite.domain.auth.repository.AuthAccountRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PasswordChangeSharedServiceImpl implements PasswordChangeSharedService {

    private final AuthAccountContextSharedService userContextSharedService;
    private final AuthAccountRepository authAccountRepository;
    private final AuthPasswordHistoryRepository passwordHistoryRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicy passwordPolicy;

    public PasswordChangeSharedServiceImpl(AuthAccountContextSharedService userContextSharedService,
                                           AuthAccountRepository authAccountRepository,
                                           AuthPasswordHistoryRepository passwordHistoryRepository,
                                           PasswordEncoder passwordEncoder,
                                           PasswordPolicy passwordPolicy) {
        this.userContextSharedService = userContextSharedService;
        this.authAccountRepository = authAccountRepository;
        this.passwordHistoryRepository = passwordHistoryRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicy = passwordPolicy;
    }

    @Override
    public void changePasswordOfCurrentUser(String currentRawPassword, String newRawPassword) {
        AuthAccount user = userContextSharedService.getCurrentUserOrThrow();
        AuthAccountId accountId = user.id();
        if (accountId == null) {
            throw new AuthDomainException("アカウントID未採番のためパスワード変更ができません。");
        }

        // 現在のパスワード検証
        if (!passwordEncoder.matches(currentRawPassword, user.encodedPassword().value())) {
            throw new PasswordPolicyViolationException("現在のパスワードが正しくありません。");
        }

        // パスワード構文チェック
        passwordPolicy.validateSyntax(newRawPassword, user.userId());

        // 履歴による再利用禁止チェック（直近 N 件）
        List<PasswordHistory> recentHistories =
                passwordHistoryRepository.findRecentByAccountId(accountId, passwordPolicy.historyGenerationCount());

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
        authAccountRepository.save(user);

        // パスワード履歴登録
        LocalDateTime now = LocalDateTime.now();
        UserId operator = user.userId(); // 自分自身が変更
        PasswordHistory history = PasswordHistory.userChange(accountId, encodedPassword, now, operator);
        passwordHistoryRepository.save(history);
    }
}
```

---

<h2>AuthAccountAdminSharedService</h2>

<h3>インタフェース</h3>

```java
package com.myou.ec.ecsite.application.auth.sharedservice;

import com.myou.ec.ecsite.domain.auth.model.AuthAccount;
import com.myou.ec.ecsite.domain.auth.model.value.AuthAccountId;
import com.myou.ec.ecsite.domain.auth.model.value.UserId;
import com.myou.ec.ecsite.domain.auth.model.value.RoleCode;

import java.util.List;

/**
 * 管理者によるアカウント管理の sharedService。
 */
public interface AuthAccountAdminSharedService {

    /**
     * アカウントを新規登録する。
     *
     * @param userId   ユーザーID
     * @param rawPassword 初期パスワード（平文）
     * @param roleCodes 付与するロール一覧
     * @param operator  操作ユーザ（管理者）の userId
     * @return 登録された AuthAccount
     */
    AuthAccount registerAccount(UserId userId,
                             String rawPassword,
                             List<RoleCode> roleCodes,
                             UserId operator);

    /**
     * 初期パスワードにリセットし、ロックも解除する。
     *
     * @param targetAccountId 対象アカウントID
     * @param operator     操作ユーザ（管理者）の userId
     */
    void resetPasswordToInitial(AuthAccountId targetAccountId, UserId operator);

    /**
     * アカウントロックを解除する。
     */
    void unlockAccount(AuthAccountId targetAccountId, UserId operator);

    /**
     * アカウントを無効化する。
     */
    void disableAccount(AuthAccountId targetAccountId, UserId operator);

    /**
     * アカウントを有効化する。
     */
    void enableAccount(AuthAccountId targetAccountId, UserId operator);
}
```

<h3>実装</h3>

```java
package com.myou.ec.ecsite.application.auth.sharedservice;

import com.myou.ec.ecsite.domain.auth.exception.AuthDomainException;
import com.myou.ec.ecsite.domain.auth.model.AccountLockEvent;
import com.myou.ec.ecsite.domain.auth.model.AuthAccount;
import com.myou.ec.ecsite.domain.auth.model.PasswordHistory;
import com.myou.ec.ecsite.domain.auth.model.value.AuthAccountId;
import com.myou.ec.ecsite.domain.auth.model.value.EncodedPassword;
import com.myou.ec.ecsite.domain.auth.model.value.UserId;
import com.myou.ec.ecsite.domain.auth.model.value.RoleCode;
import com.myou.ec.ecsite.domain.auth.model.policy.PasswordPolicy;
import com.myou.ec.ecsite.domain.auth.repository.AuthAccountLockHistoryRepository;
import com.myou.ec.ecsite.domain.auth.repository.AuthPasswordHistoryRepository;
import com.myou.ec.ecsite.domain.auth.repository.AuthRoleRepository;
import com.myou.ec.ecsite.domain.auth.repository.AuthAccountRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuthAccountAdminSharedServiceImpl implements AuthAccountAdminSharedService {

    private final AuthAccountRepository authAccountRepository;
    private final AuthRoleRepository authRoleRepository;
    private final AuthPasswordHistoryRepository passwordHistoryRepository;
    private final AuthAccountLockHistoryRepository lockHistoryRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicy passwordPolicy;
    private final String initialPassword;

    public AuthAccountAdminSharedServiceImpl(AuthAccountRepository authAccountRepository,
                                             AuthRoleRepository authRoleRepository,
                                             AuthPasswordHistoryRepository passwordHistoryRepository,
                                             AuthAccountLockHistoryRepository lockHistoryRepository,
                                             PasswordEncoder passwordEncoder,
                                             PasswordPolicy passwordPolicy,
                                             @Value("${auth.initial-password:password123}") String initialPassword) {
        this.authAccountRepository = authAccountRepository;
        this.authRoleRepository = authRoleRepository;
        this.passwordHistoryRepository = passwordHistoryRepository;
        this.lockHistoryRepository = lockHistoryRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicy = passwordPolicy;
        this.initialPassword = initialPassword;
    }

    @Override
    public AuthAccount registerAccount(UserId userId,
                                    String rawPassword,
                                    List<RoleCode> roleCodes,
                                    UserId operator) {

        LocalDateTime now = LocalDateTime.now();

        // パスワードポリシー（構文）チェック
        passwordPolicy.validateSyntax(rawPassword, userId);

        // パスワードハッシュ化
        EncodedPassword encodedPassword = new EncodedPassword(passwordEncoder.encode(rawPassword));

        // AuthAccount 作成 & 保存
        AuthAccount user = AuthAccount.newAccount(userId, encodedPassword, roleCodes, now, operator);
        authAccountRepository.save(user);

        // ID 採番後のアカウントを再取得（ID 必要なため）
        AuthAccount savedUser = authAccountRepository.findByUserId(userId)
                .orElseThrow(() -> new AuthDomainException("アカウント登録後の再取得に失敗しました。"));

        AuthAccountId accountId = savedUser.id();
        if (accountId == null) {
            throw new AuthDomainException("採番されたアカウントIDが取得できません。");
        }

        // ユーザロール設定
        authRoleRepository.saveAccountRoles(accountId, roleCodes);

        // パスワード履歴登録（初回登録）
        PasswordHistory history = PasswordHistory.initialRegister(
                accountId,
                encodedPassword,
                now,
                operator
        );
        passwordHistoryRepository.save(history);

        return savedUser;
    }

    @Override
    public void resetPasswordToInitial(AuthAccountId targetAccountId, UserId operator) {
        AuthAccount user = authAccountRepository.findById(targetAccountId)
                .orElseThrow(() -> new AuthDomainException("対象アカウントが存在しません。"));

        LocalDateTime now = LocalDateTime.now();

        // 初期パスワードの構文チェック（ポリシーに沿っている前提だが、一応検証）
        passwordPolicy.validateSyntax(initialPassword, user.userId());

        // ハッシュ化
        EncodedPassword encodedPassword = new EncodedPassword(passwordEncoder.encode(initialPassword));

        // パスワード更新
        user.changePassword(encodedPassword);
        authAccountRepository.save(user);

        // パスワード履歴（ADMIN_RESET）
        PasswordHistory history = PasswordHistory.adminReset(
                targetAccountId,
                encodedPassword,
                now,
                operator
        );
        passwordHistoryRepository.save(history);

        // ロック解除イベント（パスワード初期化時はロック解除も行う）
        AccountLockEvent unlockEvent = AccountLockEvent.unlock(
                targetAccountId,
                now,
                "ADMIN_RESET_AND_UNLOCK",
                operator
        );
        lockHistoryRepository.save(unlockEvent);
    }

    @Override
    public void unlockAccount(AuthAccountId targetAccountId, UserId operator) {
        AuthAccount user = authAccountRepository.findById(targetAccountId)
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
    public void disableAccount(AuthAccountId targetAccountId, UserId operator) {
        AuthAccount user = authAccountRepository.findById(targetAccountId)
                .orElseThrow(() -> new AuthDomainException("対象アカウントが存在しません。"));

        user.disable();
        authAccountRepository.save(user);
    }

    @Override
    public void enableAccount(AuthAccountId targetAccountId, UserId operator) {
        AuthAccount user = authAccountRepository.findById(targetAccountId)
                .orElseThrow(() -> new AuthDomainException("対象アカウントが存在しません。"));

        user.enable();
        authAccountRepository.save(user);
    }
}
```

---

これで、AP基盤側の **sharedService 3種の実装** が揃いました。

* 業務Tの Service クラスからは：

    * ログイン中情報 → `AuthAccountContextSharedService`
    * パスワード変更画面 → `PasswordChangeSharedService`
    * アカウント管理画面 → `AuthAccountAdminSharedService`
      を DI して呼び出すイメージです。

このあとやるとしたら：

* Spring Security の Success/FailureHandler から

    * `AuthLoginHistoryRepository` / `AuthAccountLockHistoryRepository` を使って履歴・ロック制御
* ログイン画面／メニュー画面の Controller 実装（前回ログイン日時の表示など）

あたりですね。必要になったらそこも一緒に詰めていきましょう。
