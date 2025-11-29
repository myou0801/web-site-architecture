sharedService 実装、ここを固めると「業務Tからどう使うか」が一気に見えてきますね。
3クラスそれぞれについて、**責務・依存・メソッド内の処理フロー**を設計していきます。

---

# 0. 前提（場所と役割）

* パッケージ：`com.myou.ec.ecsite.application.auth.sharedservice`
* 対象クラス：

    * `AuthAccountAdminSharedServiceImpl`
    * AuthAccountContextSharedServiceImpl
    * `PasswordChangeSharedServiceImpl`
* 業務Tからは **ここだけを直接呼ぶ**（Controller や Service が使う入口）。
* 中では

    * Domain の Entity/VO
    * DomainService
    * Repository
      を組み合わせてユースケースを実現。

---

# 1. AuthAccountAdminSharedServiceImpl

## 1-1. 役割

アカウント管理画面から呼ばれる、「アカウント管理用の共通サービス」です。

* 認証アカウント一覧検索（条件付き）
* 認証アカウント詳細取得
* 新規アカウント登録
* 既存アカウント更新
* パスワード初期化＋ロック解除
* ロック解除のみ

→ 業務T側は画面用 Service から DTO/Command を使ってこのクラスを呼ぶだけ、
認証ロジック／履歴／ロックの細かいところは AP基盤側に隠蔽。

## 1-2. 依存するコンポーネント

```text
Domain の Repository
- AuthAccountRepository
- AuthRoleRepository
- AuthLoginHistoryRepository（または LoginHistoryDomainService 経由）
- AuthPasswordHistoryRepository（または PasswordHistoryDomainService 経由）

DomainService
- PasswordHistoryDomainService
- AccountLockDomainService
- LoginHistoryDomainService（前回ログイン日時を出す用）

その他
- PasswordEncoder         … 初期パスワードのハッシュ化
- Clock                   … 現在日時
- String initialPassword  … 固定初期パスワード（application.properties から注入）
```

## 1-3. クラスシグネチャ例

```java
package com.myou.ec.ecsite.application.auth.sharedservice;

import com.myou.ec.ecsite.domain.auth.repository.*;
import com.myou.ec.ecsite.domain.auth.domainservice.*;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;

public class AuthAccountAdminSharedServiceImpl implements AuthAccountAdminSharedService {

    private final AuthAccountRepository authAccountRepository;
    private final AuthRoleRepository authRoleRepository;
    private final AuthLoginHistoryRepository authLoginHistoryRepository;
    private final PasswordHistoryDomainService passwordHistoryDomainService;
    private final LoginHistoryDomainService loginHistoryDomainService;
    private final AccountLockDomainService accountLockDomainService;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;
    private final String initialPassword; // "password123" など

    public AuthAccountAdminSharedServiceImpl(
            AuthAccountRepository authAccountRepository,
            AuthRoleRepository authRoleRepository,
            AuthLoginHistoryRepository authLoginHistoryRepository,
            PasswordHistoryDomainService passwordHistoryDomainService,
            LoginHistoryDomainService loginHistoryDomainService,
            AccountLockDomainService accountLockDomainService,
            PasswordEncoder passwordEncoder,
            Clock clock,
            String initialPassword
    ) {
        this.authAccountRepository = authAccountRepository;
        this.authRoleRepository = authRoleRepository;
        this.authLoginHistoryRepository = authLoginHistoryRepository;
        this.passwordHistoryDomainService = passwordHistoryDomainService;
        this.loginHistoryDomainService = loginHistoryDomainService;
        this.accountLockDomainService = accountLockDomainService;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
        this.initialPassword = initialPassword;
    }

    // 各メソッド実装は後述
}
```

## 1-4. メソッドごとの処理フロー

### (1) search(AuthAccountSearchCondition)

**やること**

1. 条件をそのまま Mapper に渡すのではなく、

    * userIdLike
    * roleCodes
    * enabled
    * locked
      に応じて Repository を組み合わせて検索
      （実務では MyBatis で検索条件ごとの SQL をまとめてもOK）
2. 取得した `AuthAccount` + ロール一覧 + ログイン履歴から、

    * lastLoginAt
    * previousLoginAt
      を計算
3. `AuthAccountSummaryDto` のリストにマッピングして `AuthAccountSearchResult` として返す

※SQL 的には専用のビュー or 複雑な JOIN でもいいですが、設計としては
**「Domain オブジェクト → DTO にマッピングする責務は sharedService」** としておきます。

### (2) findById(long authAccountId)

1. `AuthAccountRepository.findById(new AuthAccountId(authAccountId))`
2. 見つからなければ `AuthAccountNotFoundException` 的なものを投げる
3. `AuthRoleRepository.findByAccountId(authAccountId)` でロール一覧取得
4. `LoginHistoryDomainService.findPreviousSuccessLoginAt()` + 最新 SUCCESS から

    * lastLoginAt
    * previousLoginAt
5. `PasswordHistoryDomainService.findLastChangedAt()` で最終変更日時取得
6. 有効期限切れかどうかは `AuthPolicyDomainService.isPasswordExpired()` で判定
7. 上記情報から `AuthAccountDetailDto` を組み立てる

### (3) register(AuthAccountRegisterCommand, operatedByUserId)

1. `UserId` VO に変換

2. 初期パスワードをハッシュ化

   ```java
   String encoded = passwordEncoder.encode(initialPassword);
   EncodedPassword encodedPassword = new EncodedPassword(encoded);
   ```

3. `RoleCode` のリストに変換

4. `AuthAccountId` は IDENTITY なら null で作って `save()` 時に採番

5. `AuthAccount` エンティティを new

6. `authAccountRepository.save(account)`

7. 採番された `AuthAccountId` を取得

8. `PasswordHistoryDomainService.recordInitialRegister()` を呼ぶ

9. 戻り値として authAccountId を返す

### (4) update(AuthAccountUpdateCommand, operatedByUserId)

1. `AuthAccountRepository.findById()` で既存アカウント取得
2. コマンドに応じて

    * userId 変更（必要なら）
    * enabled フラグ変更
    * ロール変更（`AuthRoleRepository`/`AuthAccountRole`用 Mapper で更新）
3. `AuthAccountRepository.save()` で更新

※パスワード更新は `PasswordChangeSharedService` または管理者機能の別UseCaseに任せる想定。

### (5) resetPasswordAndUnlock(authAccountId, operatedByUserId)

1. アカウント取得
2. 初期パスワードをハッシュ化
3. `AuthAccount.changePassword()` で更新、`AuthAccountRepository.save()`
4. `PasswordHistoryDomainService.recordAdminReset()` を呼ぶ
5. `AccountLockDomainService.unlock()` を呼ぶ

    * `reason = "ADMIN_RESET_AND_UNLOCK"`
    * `operatedBy = new UserId(operatedByUserId)`
6. ※仕様通り「パスワード初期化するとロックも解除」

### (6) unlock(authAccountId, operatedByUserId)

1. `AccountLockDomainService.unlock()` を呼ぶだけ
2. `reason = "ADMIN_UNLOCK"`

---

# 2. AuthAccountContextSharedServiceImpl

## 2-1. 役割

* **現在ログイン中のアカウントコンテキスト**を業務Tに提供する。
* 業務側の Service/Controller が使う用途：

    * 今のユーザーID
    * 今のロール一覧
    * 認証アカウントの内部ID（authAccountId）
    * 前回ログイン日時

## 2-2. 依存するコンポーネント

```text
- SecurityContextHolder（Spring Security）
- LoginSharedUseCase / LoginHistoryDomainService のどちらか（前回ログイン日時のバックアップ用）
- AuthAccountRepository（authAccountId を Security の Principal が持っていない場合用）
```

### 前回ログイン日時の扱い

* 成功時に `LoginSharedUseCase` が計算した `previousLoginAt` を

    * SuccessHandler が `HttpSession` or `SecurityContext` に詰めておく
* 本クラスでは基本それを読む
  → セッションがないときは `LoginHistoryDomainService` に問い合わせる fallback

## 2-3. 想定する Principal

認証成功後、SecurityContext には例えばこんなカスタム UserDetails を使う想定：

```java
public class AuthAccountPrincipal implements UserDetails {
    private final long authAccountId;
    private final String userId;
    private final List<String> roleCodes;
    // UserDetails のメソッド実装…

    public long getAuthAccountId() { return authAccountId; }
    public String getUserId() { return userId; }
    public List<String> getRoleCodes() { return roleCodes; }
}
```

## 2-4. クラスシグネチャ例

```java
package com.myou.ec.ecsite.application.auth.sharedservice;

import com.myou.ec.ecsite.domain.auth.domainservice.LoginHistoryDomainService;
import com.myou.ec.ecsite.domain.auth.model.value.AuthAccountId;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import jakarta.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class AuthAccountContextSharedServiceImpl implements AuthAccountContextSharedService {

    private final LoginHistoryDomainService loginHistoryDomainService;
    private final HttpSession httpSession; // DI する or RequestContextHolder で取得する

    public AuthAccountContextSharedServiceImpl(LoginHistoryDomainService loginHistoryDomainService,
                                            HttpSession httpSession) {
        this.loginHistoryDomainService = loginHistoryDomainService;
        this.httpSession = httpSession;
    }

    @Override
    public long getCurrentAuthAccountId() {
        return getPrincipal().getAuthAccountId();
    }

    @Override
    public String getCurrentUserId() {
        return getPrincipal().getUserId();
    }

    @Override
    public List<String> getCurrentRoleCodes() {
        return getPrincipal().getRoleCodes();
    }

    @Override
    public Optional<LocalDateTime> getPreviousLoginAt() {
        // まずはセッションから
        Object value = httpSession.getAttribute("previousLoginAt");
        if (value instanceof LocalDateTime dt) {
            return Optional.of(dt);
        }

        // 無ければ履歴から計算（直接 DomainService を呼ぶ）
        long authAccountId = getPrincipal().getAuthAccountId();
        return loginHistoryDomainService.findPreviousSuccessLoginAt(new AuthAccountId(authAccountId));
    }

    @Override
    public CurrentAuthUserInfo getCurrentAuthUserInfo() {
        long authAccountId = getCurrentAuthAccountId();
        String userId = getCurrentUserId();
        List<String> roles = getCurrentRoleCodes();
        LocalDateTime previousLoginAt = getPreviousLoginAt().orElse(null);

        return new CurrentAuthUserInfo(authAccountId, userId, roles, previousLoginAt);
    }

    private AuthAccountPrincipal getPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (AuthAccountPrincipal) auth.getPrincipal();
    }
}```

---

# 3. PasswordChangeSharedServiceImpl

## 3-1. 役割

* **ユーザ自身がログイン後に行うパスワード変更**の共通処理
* 仕様：

    * 現在パスワード一致チェック
    * パスワードポリシー（長さ・英数字・loginIdとの一致禁止）
    * 3世代再利用禁止
    * 更新と履歴登録（USER_CHANGE）
    * 必要ならばロック解除などもここで可能だが、現仕様では「ロック解除は管理者のみ」

## 3-2. 依存するコンポーネント

```text
- AuthAccountRepository
- AuthPolicyDomainService
- PasswordHistoryDomainService
- PasswordEncoder
- Clock
```

## 3-3. クラスシグネチャ例

```java
package com.myou.ec.ecsite.application.auth.sharedservice;

import com.myou.ec.ecsite.domain.auth.domainservice.AuthPolicyDomainService;
import com.myou.ec.ecsite.domain.auth.domainservice.PasswordHistoryDomainService;
import com.myou.ec.ecsite.domain.auth.model.AuthUser;
import com.myou.ec.ecsite.domain.auth.model.value.AuthUserId;
import com.myou.ec.ecsite.domain.auth.model.value.EncodedPassword;
import com.myou.ec.ecsite.domain.auth.repository.AuthUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;

public class PasswordChangeSharedServiceImpl implements PasswordChangeSharedService {

    private final AuthAccountRepository authAccountRepository;
    private final AuthPolicyDomainService authPolicyDomainService;
    private final PasswordHistoryDomainService passwordHistoryDomainService;
    private final PasswordEncoder passwordEncoder;

    public PasswordChangeSharedServiceImpl(AuthAccountRepository authAccountRepository,
                                           AuthPolicyDomainService authPolicyDomainService,
                                           PasswordHistoryDomainService passwordHistoryDomainService,
                                           PasswordEncoder passwordEncoder) {
        this.authAccountRepository = authAccountRepository;
        this.authPolicyDomainService = authPolicyDomainService;
        this.passwordHistoryDomainService = passwordHistoryDomainService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void changeOwnPassword(PasswordChangeCommand command) {
        AuthAccountId authAccountId = new AuthAccountId(command.authAccountId());
        AuthAccount user = authAccountRepository.findById(authAccountId)
                .orElseThrow(() -> new AuthAccountNotFoundException(command.authAccountId()));

        // 1. 現在パスワード照合
        if (!passwordEncoder.matches(command.currentRawPassword(), user.encodedPassword().value())) {
            throw new BadCurrentPasswordException();
        }

        // 2. パスワードポリシー構文チェック
        authPolicyDomainService.validatePasswordSyntax(
                command.newRawPassword(),
                user.userId()
        );

        // 3. 新パスワードをハッシュ化
        String encoded = passwordEncoder.encode(command.newRawPassword());
        EncodedPassword newEncodedPassword = new EncodedPassword(encoded);

        // 4. 3世代再利用禁止
        passwordHistoryDomainService.checkNotReusedRecently(authAccountId, newEncodedPassword);

        // 5. ドメインモデル更新
        user.changePassword(newEncodedPassword);
        authAccountRepository.save(user);

        // 6. 履歴登録（USER_CHANGE）
        passwordHistoryDomainService.recordUserChange(authAccountId, newEncodedPassword, user.userId());
    }
}
```

---

# 4. ここまでの sharedService の立ち位置

* 業務Tの Service / Controller から見えるのは：

    * `AuthAccountAdminSharedService`
    * `AuthAccountContextSharedService`
    * `PasswordChangeSharedService`
* それぞれの実装クラス（Impl）が

    * DomainService
    * Repository
    * Spring Security
      を意識した処理を担当し、
      業務側からは **DTO/Command + シンプルなメソッド呼び出し** で済むようになります。

---

この sharedService 実装が見えてきたので、
次の設計としては：

* `SecurityConfig` と `CustomAuthenticationSuccessHandler` / `CustomAuthenticationFailureHandler` に
  今の `LoginSharedUseCase` / `LockControlSharedUseCase` / `AuthUserContextSharedService` をどう組み込むか
* もしくは アカウント管理画面用の `AccountController` / `AccountService`（業務T側）の I/F スケッチ

あたりをやると、画面まで一気につながる感じになります。
