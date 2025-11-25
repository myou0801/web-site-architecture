
# ecsite-auth Domain README（domainservice 最小構成版）

認証系ドメイン (`auth`) の **Value Object / Entity / Repository** の一覧と役割をまとめた README です。  
このバージョンでは、**DomainService を極力作らず**、ロジックは以下に寄せています：

- ルール・判定: **ValueObject / Entity**
- DBクエリ＋集計: **Repository**

---

## 1. パッケージ構成

```text
com.myou.ec.ecsite.domain.auth
 ├─ model              … Entity（集約ルート含む）
 │   ├─ AuthUser
 │   ├─ AuthRole
 │   ├─ LoginHistory
 │   ├─ PasswordHistory
 │   └─ AccountLockEvent
 │
 ├─ model.value        … Value Object / Enum / Policy
 │   ├─ AuthUserId
 │   ├─ LoginId
 │   ├─ EncodedPassword
 │   ├─ RoleCode
 │   ├─ LoginResult
 │   ├─ PasswordChangeType
 │   ├─ LockStatus
 │   ├─ PasswordPolicy
 │   └─ LockPolicy
 │
 └─ repository         … Repository インタフェース
     ├─ AuthUserRepository
     ├─ AuthRoleRepository
     ├─ AuthLoginHistoryRepository
     ├─ AuthPasswordHistoryRepository
     └─ AuthAccountLockHistoryRepository
````

> ※ `domainservice` パッケージは現時点では **作らない方針**。
> 必要になったら後から追加できるようにしておく。

---

## 2. Value Object / Enum / Policy

### 2.1 基本 Value Object

* `LoginId`

    * ログインID（ユーザID）を表す VO
    * 非null／非空チェックなど最低限のバリデーション

* `AuthUserId`

    * 認証ユーザID（`AUTH_USER.AUTH_USER_ID`）を表す VO
    * 正の整数のみ

* `EncodedPassword`

    * ハッシュ済みパスワードを表す VO
    * すでにエンコード済みの値のみ保持（生パスワードは保持しない）

* `RoleCode`

    * 権限・ロールを表すコード値（例: `ROLE_ADMIN`）

---

### 2.2 Enum

* `LoginResult`

    * ログイン履歴の結果種別
    * `SUCCESS` / `FAIL` / `LOCKED` / `DISABLED`

* `PasswordChangeType`

    * パスワード履歴の変更種類
    * `INITIAL_REGISTER`（初回登録）
    * `ADMIN_RESET`（管理者初期化）
    * `USER_CHANGE`（ユーザ自身による変更）

* `LockStatus`

    * アカウントのロック状態
    * `LOCKED` / `UNLOCKED`
    * 補助メソッド：

        * `isLocked()` / `isUnlocked()`

---

### 2.3 Policy VO（domainservice の代わり）

#### PasswordPolicy

パスワードポリシーを表現する VO。
旧 `AuthPolicyDomainService` の「パスワード系」責務をこちらに寄せる。

```java
public record PasswordPolicy(
        int minLength,                 // 例: 5
        int historyGenerationCount,    // 例: 3世代
        int expireDays                 // 例: 90日
) {

    public void validateSyntax(String rawPassword, LoginId loginId) {
        // 桁数、英数字のみ、ログインID完全一致禁止 等をここでチェック
        // 違反時は PasswordPolicyViolationException など Domain 例外を投げる想定
    }

    public boolean isExpired(LocalDateTime lastChangedAt, LocalDateTime now) {
        if (lastChangedAt == null) return true; // 初期化直後などの扱いは設計で決める
        return lastChangedAt.plusDays(expireDays).isBefore(now);
    }
}
```

#### LockPolicy

ロックに関するポリシーを表現する VO。

```java
public record LockPolicy(
        int failThreshold // 例: 6回
) {
    public boolean isOverThreshold(int consecutiveFailCount) {
        return consecutiveFailCount >= failThreshold;
    }
}
```

> これらの Policy は
>
> * AP基盤の sharedService でコンストラクタ注入（設定値）
> * あるいは `static final` 定数として１つだけ使い回す
    >   のどちらでもOK。

---

## 3. Entity 一覧

### 3.1 AuthUser（認証ユーザ）

認証対象となるユーザの **認証情報のみ** を保持する Entity。
ユーザ詳細（氏名・所属など）は業務側テーブルで管理。

**主なフィールド**

* `AuthUserId id`（null = 未採番）
* `LoginId loginId`
* `EncodedPassword encodedPassword`
* `boolean enabled`
* `boolean deleted`
* `List<RoleCode> roleCodes`
* 監査情報

    * `LocalDateTime createdAt`
    * `LoginId createdByLoginId`
    * `LocalDateTime updatedAt`
    * `LoginId updatedByLoginId`
    * `long versionNo`

**代表的な振る舞い**

* `changePassword(EncodedPassword newPassword, LocalDateTime now, LoginId operator)`
* `changeLoginId(LoginId newLoginId, LocalDateTime now, LoginId operator)`
* `enable(...) / disable(...) / markDeleted(...)`
* `changeRoles(List<RoleCode> newRoles, ...)`
* （必要であれば）ログイン可否の簡易判定

    * `boolean canLogin(LockStatus lockStatus)` など

> ※ ロック状態そのものは `AccountLockEvent` / `LockStatus` 側に任せ、
> `AuthUser` は enabled/deleted までに留める方針でもOK。

---

### 3.2 AuthRole（ロールマスタ）

ロールのマスタ情報を表す Entity。

**想定フィールド**

* `RoleCode roleCode`
* `String roleName`      （表示名）
* `String description`   （説明文）

**役割**

* マスタ参照用途（管理画面のロール選択、一覧表示など）
* 業務ロールが増えても、Domain 上は `AuthRole` と `RoleCode` で吸収

---

### 3.3 LoginHistory（ログイン履歴）

ログイン試行の履歴を表す Entity（基本 immutable）。

**主なフィールド**

* `Long id`
* `AuthUserId authUserId`
* `LocalDateTime loginAt`
* `LoginResult result`
* `String clientIp`
* `String userAgent`
* `LocalDateTime createdAt`
* `LoginId createdBy`

**代表的なファクトリ**

* `LoginHistory.success(...)`
* `LoginHistory.fail(...)`
* `LoginHistory.locked(...)`

---

### 3.4 PasswordHistory（パスワード履歴）

パスワード変更の履歴を表す Entity（immutable）。

**主なフィールド**

* `Long id`
* `AuthUserId authUserId`
* `EncodedPassword encodedPassword`
* `PasswordChangeType changeType`
* `LocalDateTime changedAt`
* `LoginId changedBy`
* `LocalDateTime createdAt`
* `LoginId createdBy`

**代表的なファクトリ**

* `PasswordHistory.initialRegister(...)`
* `PasswordHistory.adminReset(...)`
* `PasswordHistory.userChange(...)`

**補助ロジック（List に対して使う想定）**

* `boolean isReused(EncodedPassword newPassword, int generationCount)`

    * 直近 `generationCount` 件と newPassword を比較し、再利用なら true

これは

* Repository で `List<PasswordHistory>` を取得
* sharedService 側で静的ヘルパーやユーティリティで判定

という形にしてもOKです。

---

### 3.5 AccountLockEvent（アカウントロック履歴）

ロック／ロック解除のイベント履歴を表す Entity。

**主なフィールド**

* `Long id`
* `AuthUserId authUserId`
* `boolean locked`（true=ロック, false=ロック解除）
* `LocalDateTime occurredAt`
* `String reason`
  （例: `LOGIN_FAIL_THRESHOLD`, `ADMIN_UNLOCK`, `ADMIN_RESET_AND_UNLOCK`）
* `LoginId operatedBy`
* `LocalDateTime createdAt`
* `LoginId createdBy`

**代表的なファクトリ**

* `AccountLockEvent.lock(...)`
* `AccountLockEvent.unlock(...)`

**補助ロジック**

* List<AccountLockEvent> の最後の要素から `LockStatus` を求めるヘルパーを用意しておくと便利

    * 例：`LockStatus fromLatestEvent(AccountLockEvent latest)` など

---

## 4. Repository インタフェース

### 4.1 AuthUserRepository

```java
public interface AuthUserRepository {
    Optional<AuthUser> findById(AuthUserId id);
    Optional<AuthUser> findByLoginId(LoginId loginId);
    void save(AuthUser user);
}
```

* `AUTH_USER` テーブル + `AUTH_USER_ROLE` テーブルから
  `AuthUser` と `List<RoleCode>` を組み立てる責務は **infrastructure 層** に置く。

---

### 4.2 AuthRoleRepository

```java
public interface AuthRoleRepository {
    List<AuthRole> findAll();

    List<RoleCode> findRoleCodesByUserId(AuthUserId authUserId);

    void saveUserRoles(AuthUserId authUserId, List<RoleCode> roleCodes);
}
```

* `AUTH_ROLE` / `AUTH_USER_ROLE` を裏側で操作する窓口。
* Domain からは「ユーザに紐づく RoleCode の一覧」「全ロール一覧」だけ見える。

---

### 4.3 AuthLoginHistoryRepository

```java
public interface AuthLoginHistoryRepository {

    void save(LoginHistory history);

    List<LoginHistory> findRecentByUserId(AuthUserId userId, int limit);

    /**
     * 前回ログイン日時（今回を除く直近SUCCESS）
     */
    Optional<LocalDateTime> findPreviousSuccessLoginAt(AuthUserId userId);

    /**
     * 最後の SUCCESS または UNLOCK 以降の連続FAIL回数。
     */
    int countConsecutiveFailuresSinceLastSuccessOrUnlock(AuthUserId userId);
}
```

> ここに「ちょっと賢い」メソッドを持たせることで、
> 以前 `LoginHistoryDomainService` に置こうとしていたロジックを Repository 側に寄せる。

---

### 4.4 AuthPasswordHistoryRepository

```java
public interface AuthPasswordHistoryRepository {

    void save(PasswordHistory history);

    List<PasswordHistory> findRecentByUserId(AuthUserId userId, int limit);

    Optional<PasswordHistory> findLastByUserId(AuthUserId userId);
}
```

* 3世代再利用禁止チェックは：

    * `findRecentByUserId(userId, policy.historyGenerationCount())` で取得
    * sharedService 側で `isReused()` ヘルパーを呼んで判定
      という流れを想定。

---

### 4.5 AuthAccountLockHistoryRepository

```java
public interface AuthAccountLockHistoryRepository {

    void save(AccountLockEvent event);

    Optional<AccountLockEvent> findLatestByUserId(AuthUserId userId);

    /**
     * 最新イベントを見て LockStatus を返すユーティリティ的メソッドを
     * 置いても良い。
     */
    default LockStatus getLockStatus(AuthUserId userId) {
        return findLatestByUserId(userId)
                .map(e -> e.locked() ? LockStatus.LOCKED : LockStatus.UNLOCKED)
                .orElse(LockStatus.UNLOCKED);
    }
}
```

> こうしておけば、application/sharedService 側では
> `lockHistoryRepository.getLockStatus(userId)` と呼ぶだけで済み、
> `AccountLockDomainService` は不要になる。

---

## 5. 設計ポリシー（domainservice を作らない前提）

* **DomainService は作らない／最小限にする**

    * ルールは VO / Entity / Repository に集約
* AP基盤の sharedService / UseCase は：

    * `AuthUserRepository` など Repository
    * `PasswordPolicy` / `LockPolicy` 等の VO
      を直接使ってユースケースの流れを組み立てる。
* Domain 層は依然として **フレームワーク非依存**

    * アノテーション（`@Entity`, `@Service` 等）は付けない。


