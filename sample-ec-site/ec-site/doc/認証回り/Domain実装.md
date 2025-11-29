`domain` 配下（auth ドメイン）の実装ファイル一式と、それを説明する `README.md` をまとめて出します。
そのままコピーしてプロジェクトに貼れる形にしておきます。

---

## README.md（domain/auth 用）

````markdown
# ecsite-auth Domain

認証系ドメイン (`auth`) の実装です。  
DDD 方針に基づき、**ドメインサービスは極力作らず**、

- ルール・判定 → ValueObject / Entity / Policy
- 履歴や集計 → Repository インタフェース経由

に寄せています。

## パッケージ構成

```text
com.myou.ec.ecsite.domain.auth
 ├─ AuthDomainException                … 認証ドメイン共通の基底例外
 ├─ PasswordPolicyViolationException   … パスワードポリシー違反
 ├─ PasswordReuseNotAllowedException   … パスワード再利用禁止違反
 ├─ AccountLockedException             … ロック中アカウントのログインなど
 │
 ├─ model
 │   ├─ AuthAccount                       … 認証ユーザ Entity（認証情報のみ）
 │   ├─ AuthRole                       … ロールマスタ Entity
 │   ├─ LoginHistory                   … ログイン履歴 Entity
 │   ├─ PasswordHistory                … パスワード履歴 Entity
 │   └─ AccountLockEvent               … ロック／解除イベント Entity
 │
 ├─ model.value
 │   ├─ AuthAccountId                     … 認証ユーザID VO
 │   ├─ UserId                        … ログインID VO
 │   ├─ EncodedPassword                … ハッシュ済みパスワード VO
 │   ├─ RoleCode                       … ロールコード VO
 │   ├─ LoginResult                    … ログイン結果種別
 │   ├─ PasswordChangeType             … パスワード変更種別
 │   ├─ LockStatus                     … ロック状態（LOCKED/UNLOCKED）
 │   ├─ PasswordPolicy                 … パスワードポリシー（最小桁数・有効期限・世代数）
 │   └─ LockPolicy                     … ロックポリシー（失敗閾値）
 │
 └─ repository
     ├─ AuthAccountRepository             … 認証ユーザ永続化 I/F
     ├─ AuthRoleRepository             … ロールマスタ & ユーザロール I/F
     ├─ AuthLoginHistoryRepository     … ログイン履歴 I/F（連続失敗数などの集計も担当）
     ├─ AuthPasswordHistoryRepository  … パスワード履歴 I/F
     └─ AuthAccountLockHistoryRepository … ロック履歴 I/F（LockStatus 判定も担当）
````

## 設計方針メモ

* ドメイン層はフレームワーク非依存（Spring アノテーションなどは付けない）
* 監査カラム（createdAt / updatedAt / versionNo 等）の値は、

    * ドメイン側では「存在するもの」として扱い、
    * 実際の値の更新は infrastructure 層（Record + Mapper + RepositoryImpl）で行う方針
* ロック状態やパスワード有効期限などのポリシーは `PasswordPolicy` / `LockPolicy` VO に閉じ込め、
  AP 基盤の sharedService / UseCase でこれらを利用して判定する。
* `AuthAccount` は「認証に必要な最小限の情報（ID／ログインID／ハッシュパスワード／enabled／deleted／roles）」のみを持ち、
  氏名・所属などの業務属性は別ドメイン（業務T）で管理する。

````

---

## 例外クラス

### AuthDomainException.java

```java
package com.myou.ec.ecsite.domain.auth;

/**
 * 認証ドメイン共通の基底例外。
 */
public class AuthDomainException extends RuntimeException {

    public AuthDomainException(String message) {
        super(message);
    }

    public AuthDomainException(String message, Throwable cause) {
        super(message, cause);
    }
}
````

### PasswordPolicyViolationException.java

```java
package com.myou.ec.ecsite.domain.auth;

/**
 * パスワードポリシー違反（構文NGなど）の例外。
 */
public class PasswordPolicyViolationException extends AuthDomainException {

    public PasswordPolicyViolationException(String message) {
        super(message);
    }
}
```

### PasswordReuseNotAllowedException.java

```java
package com.myou.ec.ecsite.domain.auth;

/**
 * パスワード再利用禁止（履歴ルール違反）の例外。
 */
public class PasswordReuseNotAllowedException extends AuthDomainException {

    public PasswordReuseNotAllowedException() {
        super("パスワードは直近の履歴と同じ値は利用できません。");
    }
}
```

### AccountLockedException.java

```java
package com.myou.ec.ecsite.domain.auth;

/**
 * ロック中アカウントに対するログインなどの例外。
 */
public class AccountLockedException extends AuthDomainException {

    public AccountLockedException() {
        super("アカウントがロックされています。");
    }
}
```

---

## Value Object / Enum / Policy

### UserId.java

```java
package com.myou.ec.ecsite.domain.auth.model.value;

/**
 * ログインID（ユーザID）を表す値オブジェクト。
 */
public record UserId(String value) {

    public UserId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("userId must not be blank.");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
```

### AuthAccountId.java

```java
package com.myou.ec.ecsite.domain.auth.model.value;

/**
 * 認証ユーザIDを表す値オブジェクト。
 */
public record AuthAccountId(long value) {

    public AuthAccountId {
        if (value <= 0) {
            throw new IllegalArgumentException("authAccountId must be positive.");
        }
    }

    @Override
    public String toString() {
        return Long.toString(value);
    }
}
```

### EncodedPassword.java

```java
package com.myou.ec.ecsite.domain.auth.model.value;

/**
 * ハッシュ済みパスワードを表す値オブジェクト。
 * 生パスワードはここでは扱わない。
 */
public record EncodedPassword(String value) {

    public EncodedPassword {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("encodedPassword must not be blank.");
        }
    }

    @Override
    public String toString() {
        // toStringで中身をそのまま出さないようにしておく。
        return "*****";
    }
}
```

### RoleCode.java

```java
package com.myou.ec.ecsite.domain.auth.model.value;

/**
 * 権限ロールコードを表す値オブジェクト。
 * 例: ROLE_ADMIN, ROLE_USER 等。
 */
public record RoleCode(String value) {

    public RoleCode {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("roleCode must not be blank.");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
```

### LoginResult.java

```java
package com.myou.ec.ecsite.domain.auth.model.value;

/**
 * ログイン試行の結果種別。
 */
public enum LoginResult {
    SUCCESS,
    FAIL,
    LOCKED,
    DISABLED
}
```

### PasswordChangeType.java

```java
package com.myou.ec.ecsite.domain.auth.model.value;

/**
 * パスワード変更の種類。
 */
public enum PasswordChangeType {
    INITIAL_REGISTER,   // 初回登録
    ADMIN_RESET,        // 管理者による初期化
    USER_CHANGE         // ユーザ自身による変更
}
```

### LockStatus.java

```java
package com.myou.ec.ecsite.domain.auth.model.value;

/**
 * アカウントのロック状態。
 */
public enum LockStatus {
    LOCKED,
    UNLOCKED;

    public boolean isLocked() {
        return this == LOCKED;
    }

    public boolean isUnlocked() {
        return this == UNLOCKED;
    }
}
```

### PasswordPolicy.java

```java
package com.myou.ec.ecsite.domain.auth.model.value;

import com.myou.ec.ecsite.domain.auth.exception.PasswordPolicyViolationException;

import java.time.LocalDateTime;

/**
 * パスワードポリシーを表現する値オブジェクト。
 * - 最小桁数
 * - 履歴世代数
 * - 有効期限日数
 */
public record PasswordPolicy(
        int minLength,
        int historyGenerationCount,
        int expireDays
) {

    /**
     * パスワード構文チェック。
     * - 最小桁数
     * - 英数字のみ
     * - ログインIDとの完全一致禁止
     *
     * 違反時は PasswordPolicyViolationException を送出する。
     */
    public void validateSyntax(String rawPassword, UserId userId) {
        if (rawPassword == null || rawPassword.length() < minLength) {
            throw new PasswordPolicyViolationException(
                    "パスワードは " + minLength + " 文字以上で入力してください。"
            );
        }

        // 英数字のみ
        if (!rawPassword.matches("^[0-9A-Za-z]+$")) {
            throw new PasswordPolicyViolationException(
                    "パスワードは英数字のみ利用できます。"
            );
        }

        // ログインIDと同一禁止
        if (userId != null && rawPassword.equals(userId.value())) {
            throw new PasswordPolicyViolationException(
                    "ログインIDと同じパスワードは利用できません。"
            );
        }
    }

    /**
     * パスワード有効期限切れかどうかを判定する。
     *
     * @param lastChangedAt 最終パスワード変更日時
     * @param now           現在日時
     * @return true: 有効期限切れ
     */
    public boolean isExpired(LocalDateTime lastChangedAt, LocalDateTime now) {
        if (lastChangedAt == null) {
            // 初期パスワードなどは有効期限切れと扱う方針もあり得る
            return true;
        }
        return lastChangedAt.plusDays(expireDays).isBefore(now);
    }
}
```

### LockPolicy.java

```java
package com.myou.ec.ecsite.domain.auth.model.value;

/**
 * アカウントロックのポリシー。
 * 例: 6回失敗でロック。
 */
public record LockPolicy(
        int failThreshold
) {

    /**
     * 連続失敗回数が閾値を超えたかどうか。
     */
    public boolean isOverThreshold(int consecutiveFailCount) {
        return consecutiveFailCount >= failThreshold;
    }
}
```

---

## Entity

### AuthAccount.java

```java
package com.myou.ec.ecsite.domain.auth.model;

import com.myou.ec.ecsite.domain.auth.model.value.AuthAccountId;
import com.myou.ec.ecsite.domain.auth.model.value.EncodedPassword;
import com.myou.ec.ecsite.domain.auth.model.value.UserId;
import com.myou.ec.ecsite.domain.auth.model.value.RoleCode;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 認証ユーザ Entity（認証に必要な情報のみを保持）。
 *
 * ユーザの氏名・所属などの業務的な属性は、別ドメイン（業務T側）の
 * アカウント詳細テーブルで管理する前提。
 */
public class AuthAccount {

    /** AUTH_ACCOUNT_ID。null の場合は未採番。 */
    private final AuthAccountId id;

    /** ログインID。 */
    private UserId userId;

    /** ハッシュ済みパスワード。 */
    private EncodedPassword encodedPassword;

    /** 有効フラグ。false の場合はログイン不可。 */
    private boolean enabled;

    /** 論理削除フラグ。true の場合は無効ユーザ扱い。 */
    private boolean deleted;

    /** 付与されているロール一覧。 */
    private List<RoleCode> roleCodes;

    // 監査情報
    private final LocalDateTime createdAt;
    private final UserId createdByUserId;
    private final LocalDateTime updatedAt;
    private final UserId updatedByUserId;
    private final long versionNo;

    /**
     * 永続化層からの再構築などに使うコンストラクタ。
     * newAccount(...) などのファクトリを通して生成するのが基本。
     */
    public AuthAccount(AuthAccountId id,
                    UserId userId,
                    EncodedPassword encodedPassword,
                    boolean enabled,
                    boolean deleted,
                    List<RoleCode> roleCodes,
                    LocalDateTime createdAt,
                    UserId createdByUserId,
                    LocalDateTime updatedAt,
                    UserId updatedByUserId,
                    long versionNo) {

        this.id = id;
        this.userId = Objects.requireNonNull(userId, "userId must not be null");
        this.encodedPassword = Objects.requireNonNull(encodedPassword, "encodedPassword must not be null");
        this.enabled = enabled;
        this.deleted = deleted;
        this.roleCodes = roleCodes == null ? List.of() : List.copyOf(roleCodes);

        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.createdByUserId = Objects.requireNonNull(createdByUserId, "createdByUserId must not be null");
        this.updatedAt = updatedAt != null ? updatedAt : createdAt;
        this.updatedByUserId = updatedByUserId != null ? updatedByUserId : createdByUserId;
        this.versionNo = versionNo;
    }

    /**
     * 新規ユーザ作成用のファクトリメソッド。
     * まだ ID は採番されていない（id == null）状態で生成する。
     */
    public static AuthAccount newAccount(UserId userId,
                                   EncodedPassword encodedPassword,
                                   List<RoleCode> roleCodes,
                                   LocalDateTime now,
                                   UserId operator) {

        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(encodedPassword, "encodedPassword must not be null");
        Objects.requireNonNull(now, "now must not be null");
        Objects.requireNonNull(operator, "operator must not be null");

        return new AuthAccount(
                null,                     // id (未採番)
                userId,
                encodedPassword,
                true,                     // enabled デフォルト true
                false,                    // deleted デフォルト false
                roleCodes,
                now,
                operator,
                now,
                operator,
                0L                        // versionNo 初期値
        );
    }

    // ===== ビジネス振る舞い =====

    /**
     * パスワードを変更する。
     * 監査カラム（updatedAt/updatedBy/version）はインフラ層で更新する想定。
     */
    public void changePassword(EncodedPassword newPassword) {
        this.encodedPassword = Objects.requireNonNull(newPassword, "newPassword must not be null");
    }

    /**
     * ログインIDを変更する。
     */
    public void changeUserId(UserId newUserId) {
        this.userId = Objects.requireNonNull(newUserId, "newUserId must not be null");
    }

    /**
     * アカウントを有効にする。
     */
    public void enable() {
        this.enabled = true;
    }

    /**
     * アカウントを無効にする。
     */
    public void disable() {
        this.enabled = false;
    }

    /**
     * アカウントを論理削除状態にする。
     */
    public void markDeleted() {
        this.deleted = true;
    }

    /**
     * ロールを差し替える。
     */
    public void changeRoles(List<RoleCode> newRoles) {
        this.roleCodes = newRoles == null ? List.of() : List.copyOf(newRoles);
    }

    /**
     * ロールを1つ追加する（重複チェックは呼び出し側で必要に応じて行う）。
     */
    public void addRole(RoleCode roleCode) {
        Objects.requireNonNull(roleCode, "roleCode must not be null");
        List<RoleCode> copy = new ArrayList<>(roleCodes);
        copy.add(roleCode);
        this.roleCodes = List.copyOf(copy);
    }

    /**
     * ロールを1つ削除する。
     */
    public void removeRole(RoleCode roleCode) {
        Objects.requireNonNull(roleCode, "roleCode must not be null");
        List<RoleCode> copy = new ArrayList<>(roleCodes);
        copy.remove(roleCode);
        this.roleCodes = List.copyOf(copy);
    }

    /**
     * ログイン可能かどうかの簡易判定（ロック状態は別途 LockStatus で判定）。
     */
    public boolean canLogin() {
        return enabled && !deleted;
    }

    // ===== getter =====

    public AuthAccountId id() {
        return id;
    }

    public UserId userId() {
        return userId;
    }

    public EncodedPassword encodedPassword() {
        return encodedPassword;
    }

    public boolean enabled() {
        return enabled;
    }

    public boolean deleted() {
        return deleted;
    }

    public List<RoleCode> roleCodes() {
        return roleCodes;
    }

    public LocalDateTime createdAt() {
        return createdAt;
    }

    public UserId createdByUserId() {
        return createdByUserId;
    }

    public LocalDateTime updatedAt() {
        return updatedAt;
    }

    public UserId updatedByUserId() {
        return updatedByUserId;
    }

    public long versionNo() {
        return versionNo;
    }

    // ===== equals / hashCode / toString =====

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AuthAccount authAccount)) return false;
        // 永続化後は ID で等価判定。未採番同士はインスタンス等価のみ。
        return id != null && id.equals(authAccount.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : System.identityHashCode(this);
    }

    @Override
    public String toString() {
        return "AuthAccount{" +
               "id=" + id +
               ", userId=" + userId +
               ", enabled=" + enabled +
               ", deleted=" + deleted +
               ", roleCodes=" + roleCodes +
               ", versionNo=" + versionNo +
               '}';
    }
}
```

### AuthRole.java

```java
package com.myou.ec.ecsite.domain.auth.model;

import com.myou.ec.ecsite.domain.auth.model.value.RoleCode;

import java.util.Objects;

/**
 * ロールマスタ Entity。
 * 画面表示・権限管理用のマスタ。
 */
public class AuthRole {

    private final RoleCode roleCode;
    private final String roleName;
    private final String description;

    public AuthRole(RoleCode roleCode, String roleName, String description) {
        this.roleCode = Objects.requireNonNull(roleCode, "roleCode must not be null");
        this.roleName = Objects.requireNonNull(roleName, "roleName must not be null");
        this.description = description;
    }

    public RoleCode roleCode() {
        return roleCode;
    }

    public String roleName() {
        return roleName;
    }

    public String description() {
        return description;
    }
}
```

### LoginHistory.java

```java
package com.myou.ec.ecsite.domain.auth.model;

import com.myou.ec.ecsite.domain.auth.model.value.AuthAccountId;
import com.myou.ec.ecsite.domain.auth.model.value.UserId;
import com.myou.ec.ecsite.domain.auth.model.value.LoginResult;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * ログイン試行の履歴を表す Entity（基本 immutable）。
 */
public class LoginHistory {

    private final Long id;
    private final AuthAccountId authAccountId;
    private final LocalDateTime loginAt;
    private final LoginResult result;
    private final String clientIp;
    private final String userAgent;

    private final LocalDateTime createdAt;
    private final UserId createdBy;

    public LoginHistory(Long id,
                        AuthAccountId authAccountId,
                        LocalDateTime loginAt,
                        LoginResult result,
                        String clientIp,
                        String userAgent,
                        LocalDateTime createdAt,
                        UserId createdBy) {

        this.id = id;
        this.authAccountId = Objects.requireNonNull(authAccountId, "authAccountId must not be null");
        this.loginAt = Objects.requireNonNull(loginAt, "loginAt must not be null");
        this.result = Objects.requireNonNull(result, "result must not be null");
        this.clientIp = clientIp;
        this.userAgent = userAgent;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.createdBy = Objects.requireNonNull(createdBy, "createdBy must not be null");
    }

    public static LoginHistory success(AuthAccountId authAccountId,
                                       LocalDateTime loginAt,
                                       String clientIp,
                                       String userAgent,
                                       UserId createdBy) {
        return new LoginHistory(null, authAccountId, loginAt, LoginResult.SUCCESS,
                clientIp, userAgent, loginAt, createdBy);
    }

    public static LoginHistory fail(AuthAccountId authAccountId,
                                    LocalDateTime loginAt,
                                    String clientIp,
                                    String userAgent,
                                    UserId createdBy) {
        return new LoginHistory(null, authAccountId, loginAt, LoginResult.FAIL,
                clientIp, userAgent, loginAt, createdBy);
    }

    public static LoginHistory locked(AuthAccountId authAccountId,
                                      LocalDateTime loginAt,
                                      String clientIp,
                                      String userAgent,
                                      UserId createdBy) {
        return new LoginHistory(null, authAccountId, loginAt, LoginResult.LOCKED,
                clientIp, userAgent, loginAt, createdBy);
    }

    public Long id() {
        return id;
    }

    public AuthAccountId authAccountId() {
        return authAccountId;
    }

    public LocalDateTime loginAt() {
        return loginAt;
    }

    public LoginResult result() {
        return result;
    }

    public String clientIp() {
        return clientIp;
    }

    public String userAgent() {
        return userAgent;
    }

    public LocalDateTime createdAt() {
        return createdAt;
    }

    public UserId createdBy() {
        return createdBy;
    }
}
```

### PasswordHistory.java

```java
package com.myou.ec.ecsite.domain.auth.model;

import com.myou.ec.ecsite.domain.auth.model.value.AuthAccountId;
import com.myou.ec.ecsite.domain.auth.model.value.EncodedPassword;
import com.myou.ec.ecsite.domain.auth.model.value.UserId;
import com.myou.ec.ecsite.domain.auth.model.value.PasswordChangeType;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * パスワード変更履歴 Entity（immutable）。
 */
public class PasswordHistory {

    private final Long id;
    private final AuthAccountId authAccountId;
    private final EncodedPassword encodedPassword;
    private final PasswordChangeType changeType;
    private final LocalDateTime changedAt;
    private final UserId changedBy;
    private final LocalDateTime createdAt;
    private final UserId createdBy;

    public PasswordHistory(Long id,
                           AuthAccountId authAccountId,
                           EncodedPassword encodedPassword,
                           PasswordChangeType changeType,
                           LocalDateTime changedAt,
                           UserId changedBy,
                           LocalDateTime createdAt,
                           UserId createdBy) {

        this.id = id;
        this.authAccountId = Objects.requireNonNull(authAccountId, "authAccountId must not be null");
        this.encodedPassword = Objects.requireNonNull(encodedPassword, "encodedPassword must not be null");
        this.changeType = Objects.requireNonNull(changeType, "changeType must not be null");
        this.changedAt = Objects.requireNonNull(changedAt, "changedAt must not be null");
        this.changedBy = Objects.requireNonNull(changedBy, "changedBy must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.createdBy = Objects.requireNonNull(createdBy, "createdBy must not be null");
    }

    public static PasswordHistory initialRegister(AuthAccountId authAccountId,
                                                  EncodedPassword password,
                                                  LocalDateTime now,
                                                  UserId operator) {
        return new PasswordHistory(null, authAccountId, password,
                PasswordChangeType.INITIAL_REGISTER, now, operator, now, operator);
    }

    public static PasswordHistory adminReset(AuthAccountId authAccountId,
                                             EncodedPassword password,
                                             LocalDateTime now,
                                             UserId operator) {
        return new PasswordHistory(null, authAccountId, password,
                PasswordChangeType.ADMIN_RESET, now, operator, now, operator);
    }

    public static PasswordHistory userChange(AuthAccountId authAccountId,
                                             EncodedPassword password,
                                             LocalDateTime now,
                                             UserId operator) {
        return new PasswordHistory(null, authAccountId, password,
                PasswordChangeType.USER_CHANGE, now, operator, now, operator);
    }

    public Long id() {
        return id;
    }

    public AuthAccountId authAccountId() {
        return authAccountId;
    }

    public EncodedPassword encodedPassword() {
        return encodedPassword;
    }

    public PasswordChangeType changeType() {
        return changeType;
    }

    public LocalDateTime changedAt() {
        return changedAt;
    }

    public UserId changedBy() {
        return changedBy;
    }

    public LocalDateTime createdAt() {
        return createdAt;
    }

    public UserId createdBy() {
        return createdBy;
    }
}
```

### AccountLockEvent.java

```java
package com.myou.ec.ecsite.domain.auth.model;

import com.myou.ec.ecsite.domain.auth.model.value.AuthAccountId;
import com.myou.ec.ecsite.domain.auth.model.value.UserId;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * アカウントロック／ロック解除のイベント履歴 Entity。
 */
public class AccountLockEvent {

    private final Long id;
    private final AuthAccountId authAccountId;
    private final boolean locked;          // true=ロック / false=ロック解除
    private final LocalDateTime occurredAt;
    private final String reason;           // 例: LOGIN_FAIL_THRESHOLD, ADMIN_UNLOCK 等
    private final UserId operatedBy;
    private final LocalDateTime createdAt;
    private final UserId createdBy;

    public AccountLockEvent(Long id,
                            AuthAccountId authAccountId,
                            boolean locked,
                            LocalDateTime occurredAt,
                            String reason,
                            UserId operatedBy,
                            LocalDateTime createdAt,
                            UserId createdBy) {

        this.id = id;
        this.authAccountId = Objects.requireNonNull(authAccountId, "authAccountId must not be null");
        this.locked = locked;
        this.occurredAt = Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        this.reason = reason;
        this.operatedBy = Objects.requireNonNull(operatedBy, "operatedBy must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.createdBy = Objects.requireNonNull(createdBy, "createdBy must not be null");
    }

    public static AccountLockEvent lock(AuthAccountId authAccountId,
                                        LocalDateTime now,
                                        String reason,
                                        UserId operatedBy) {
        return new AccountLockEvent(null, authAccountId, true, now, reason, operatedBy, now, operatedBy);
    }

    public static AccountLockEvent unlock(AuthAccountId authAccountId,
                                          LocalDateTime now,
                                          String reason,
                                          UserId operatedBy) {
        return new AccountLockEvent(null, authAccountId, false, now, reason, operatedBy, now, operatedBy);
    }

    public Long id() {
        return id;
    }

    public AuthAccountId authAccountId() {
        return authAccountId;
    }

    public boolean locked() {
        return locked;
    }

    public LocalDateTime occurredAt() {
        return occurredAt;
    }

    public String reason() {
        return reason;
    }

    public UserId operatedBy() {
        return operatedBy;
    }

    public LocalDateTime createdAt() {
        return createdAt;
    }

    public UserId createdBy() {
        return createdBy;
    }
}
```

---

## Repository インタフェース

### AuthAccountRepository.java

```java
package com.myou.ec.ecsite.domain.auth.repository;

import com.myou.ec.ecsite.domain.auth.model.AuthAccount;
import com.myou.ec.ecsite.domain.auth.model.value.AuthAccountId;
import com.myou.ec.ecsite.domain.auth.model.value.UserId;

import java.util.Optional;

/**
 * 認証ユーザの永続化インタフェース。
 */
public interface AuthAccountRepository {

    Optional<AuthAccount> findById(AuthAccountId id);

    Optional<AuthAccount> findByUserId(UserId userId);

    void save(AuthAccount user);
}
```

### AuthRoleRepository.java

```java
package com.myou.ec.ecsite.domain.auth.repository;

import com.myou.ec.ecsite.domain.auth.model.AuthRole;
import com.myou.ec.ecsite.domain.auth.model.value.AuthAccountId;
import com.myou.ec.ecsite.domain.auth.model.value.RoleCode;

import java.util.List;

/**
 * ロールマスタおよびユーザロール関連の永続化インタフェース。
 */
public interface AuthRoleRepository {

    /**
     * 全ロール一覧を取得する。
     */
    List<AuthRole> findAll();

    /**
     * ユーザに紐づくロールコード一覧を取得する。
     */
    List<RoleCode> findRoleCodesByAccountId(AuthAccountId authAccountId);

    /**
     * ユーザに紐づくロールを差し替える。
     */
    void saveAccountRoles(AuthAccountId authAccountId, List<RoleCode> roleCodes);
}
```

### AuthLoginHistoryRepository.java

```java
package com.myou.ec.ecsite.domain.auth.repository;

import com.myou.ec.ecsite.domain.auth.model.LoginHistory;
import com.myou.ec.ecsite.domain.auth.model.value.AuthAccountId;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * ログイン履歴の永続化インタフェース。
 */
public interface AuthLoginHistoryRepository {

    void save(LoginHistory history);

    List<LoginHistory> findRecentByAccountId(AuthAccountId accountId, int limit);

    /**
     * 前回ログイン日時（今回を除く直近 SUCCESS）を返す。
     */
    Optional<LocalDateTime> findPreviousSuccessLoginAtByAccountId(AuthAccountId accountId);

    /**
     * 最後の SUCCESS または UNLOCK 以降の連続 FAIL 回数。
     */
    int countConsecutiveFailuresSinceLastSuccessOrUnlockByAccountId(AuthAccountId accountId);
}
```

### AuthPasswordHistoryRepository.java

```java
package com.myou.ec.ecsite.domain.auth.repository;

import com.myou.ec.ecsite.domain.auth.model.PasswordHistory;
import com.myou.ec.ecsite.domain.auth.model.value.AuthAccountId;

import java.util.List;
import java.util.Optional;

/**
 * パスワード履歴の永続化インタフェース。
 */
public interface AuthPasswordHistoryRepository {

    void save(PasswordHistory history);

    List<PasswordHistory> findRecentByAccountId(AuthAccountId accountId, int limit);

    Optional<PasswordHistory> findLastByAccountId(AuthAccountId accountId);
}
```

### AuthAccountLockHistoryRepository.java

```java
package com.myou.ec.ecsite.domain.auth.repository;

import com.myou.ec.ecsite.domain.auth.model.AccountLockEvent;
import com.myou.ec.ecsite.domain.auth.model.value.AuthAccountId;
import com.myou.ec.ecsite.domain.auth.model.value.LockStatus;

import java.util.Optional;

/**
 * アカウントロック／解除履歴の永続化インタフェース。
 */
public interface AuthAccountLockHistoryRepository {

    void save(AccountLockEvent event);

    Optional<AccountLockEvent> findLatestByAccountId(AuthAccountId accountId);

    /**
     * 最新のロックイベントからロック状態を返すユーティリティメソッド。
     * レコードが存在しない場合は UNLOCKED とみなす。
     */
    default LockStatus getLockStatusByAccountId(AuthAccountId accountId) {
        return findLatestByAccountId(accountId)
                .map(e -> e.locked() ? LockStatus.LOCKED : LockStatus.UNLOCKED)
                .orElse(LockStatus.UNLOCKED);
    }
}
```

---

これで `domain/auth` 配下に必要なファイルはひと通り揃っています。
次のフェーズは、このドメインに合わせて **infrastructure の Record / Mapper / RepositoryImpl** を実装していく形ですね。

もし「Gradle モジュール構成に合わせたパス一覧が欲しい」「テストコードのひな形も作りたい」などあれば、そこも続けて用意できます。
