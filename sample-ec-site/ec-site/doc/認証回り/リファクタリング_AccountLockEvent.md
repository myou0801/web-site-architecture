いいですね、AccountLockEvents まで作るとだいぶ DDD っぽく気持ちよくなります 👍
「どこをどう直すか」が分かるように、**差分ポイントごとにコード**出します。

---

## 1. Domain: `AccountLockEvents` ファーストクラスコレクション

パッケージ例：`com.myou.ec.ecsite.domain.auth.model`

```java
package com.myou.ec.ecsite.domain.auth.model;

import com.myou.ec.ecsite.domain.auth.model.value.LockStatus;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * AccountLockEvent のファーストクラスコレクション。
 *
 * - occurredAt 降順で正規化して保持
 * - 「現在のロック状態」「最後の UNLOCK 時刻」などの判定ロジックを集約
 */
public class AccountLockEvents {

    private final List<AccountLockEvent> values; // occurredAt 降順

    private AccountLockEvents(List<AccountLockEvent> values) {
        this.values = List.copyOf(
                Objects.requireNonNull(values, "values must not be null")
                        .stream()
                        .sorted(Comparator.comparing(AccountLockEvent::occurredAt).reversed())
                        .toList()
        );
    }

    public static AccountLockEvents of(List<AccountLockEvent> values) {
        return new AccountLockEvents(values);
    }

    /**
     * 現在のロック状態。
     * - イベントが1件もなければ UNLOCKED
     * - 最新イベントが locked=true なら LOCKED
     * - 最新イベントが locked=false なら UNLOCKED
     */
    public LockStatus currentStatus() {
        return values.stream()
                .findFirst()
                .map(AccountLockEvent::toLockStatus)
                .orElse(LockStatus.UNLOCKED);
    }

    /**
     * 最後（最新）のロック解除イベントの時刻。
     * - locked=false のイベントのうち、最も新しい occurredAt。
     */
    public Optional<LocalDateTime> lastUnlockAt() {
        return values.stream()
                .filter(event -> !event.locked())
                .findFirst()
                .map(AccountLockEvent::occurredAt);
    }

    public List<AccountLockEvent> asList() {
        return values;
    }
}
```

※ `AccountLockEvent` に `toLockStatus()` を追加しておくとスッキリします。

```java
public LockStatus toLockStatus() {
    return locked ? LockStatus.LOCKED : LockStatus.UNLOCKED;
}
```

---

## 2. Domain: `AuthAccountLockHistoryRepository` の修正

### 2-1. インターフェース

「ロック状態の判定ロジック」は Repository から外し、**イベントの取得だけ**にします。

```java
package com.myou.ec.ecsite.domain.auth.repository;

import com.myou.ec.ecsite.domain.auth.model.AccountLockEvent;
import com.myou.ec.ecsite.domain.auth.model.value.AuthUserId;

import java.util.List;

public interface AuthAccountLockHistoryRepository {

    void save(AccountLockEvent event);

    /**
     * 対象ユーザのロック／解除イベント一覧を取得。
     * 時系列のソートは infrastructure / AccountLockEvents 側で正規化する前提。
     */
    List<AccountLockEvent> findByUserId(AuthUserId userId);
}
```

---

## 3. Infrastructure: RepositoryImpl / Mapper 修正

### 3-1. Mapper インターフェース

```java
package com.myou.ec.ecsite.infrastructure.auth.mapper;

import com.myou.ec.ecsite.infrastructure.auth.record.AuthAccountLockHistoryRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AuthAccountLockHistoryMapper {

    void insert(AuthAccountLockHistoryRecord record);

    List<AuthAccountLockHistoryRecord> findByUserId(@Param("authUserId") long authUserId);
}
```

### 3-2. RepositoryImpl

```java
package com.myou.ec.ecsite.infrastructure.auth.repository;

import com.myou.ec.ecsite.domain.auth.model.AccountLockEvent;
import com.myou.ec.ecsite.domain.auth.model.value.AuthUserId;
import com.myou.ec.ecsite.domain.auth.repository.AuthAccountLockHistoryRepository;
import com.myou.ec.ecsite.infrastructure.auth.mapper.AuthAccountLockHistoryMapper;
import com.myou.ec.ecsite.infrastructure.auth.record.AuthAccountLockHistoryRecord;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class AuthAccountLockHistoryRepositoryImpl implements AuthAccountLockHistoryRepository {

    private final AuthAccountLockHistoryMapper mapper;

    public AuthAccountLockHistoryRepositoryImpl(AuthAccountLockHistoryMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void save(AccountLockEvent event) {
        AuthAccountLockHistoryRecord record = AuthAccountLockHistoryRecord.fromDomain(event);
        mapper.insert(record);
    }

    @Override
    public List<AccountLockEvent> findByUserId(AuthUserId userId) {
        return mapper.findByUserId(userId.value()).stream()
                .map(AuthAccountLockHistoryRecord::toDomain)
                .toList();
    }
}
```

### 3-3. MyBatis XML（イメージ）

```xml
<mapper namespace="com.myou.ec.ecsite.infrastructure.auth.mapper.AuthAccountLockHistoryMapper">

    <resultMap id="AuthAccountLockHistoryRecordMap"
               type="com.myou.ec.ecsite.infrastructure.auth.record.AuthAccountLockHistoryRecord">

        <id     column="AUTH_ACCOUNT_LOCK_HISTORY_ID" property="authAccountLockHistoryId"/>
        <result column="AUTH_USER_ID"                 property="authUserId"/>
        <result column="LOCKED_FLG"                   property="locked"/>
        <result column="OCCURRED_AT"                  property="occurredAt"/>
        <result column="REASON"                       property="reason"/>
        <result column="OPERATED_BY"                  property="operatedBy"/>
        <result column="CREATED_AT"                   property="createdAt"/>
        <result column="CREATED_BY"                   property="createdBy"/>
    </resultMap>

    <insert id="insert"
            parameterType="com.myou.ec.ecsite.infrastructure.auth.record.AuthAccountLockHistoryRecord"
            useGeneratedKeys="true"
            keyProperty="authAccountLockHistoryId">
        INSERT INTO AUTH_ACCOUNT_LOCK_HISTORY (
          AUTH_USER_ID,
          LOCKED_FLG,
          OCCURRED_AT,
          REASON,
          OPERATED_BY,
          CREATED_AT,
          CREATED_BY
        ) VALUES (
          #{authUserId},
          #{locked},
          #{occurredAt},
          #{reason},
          #{operatedBy},
          #{createdAt},
          #{createdBy}
        )
    </insert>

    <select id="findByUserId"
            resultMap="AuthAccountLockHistoryRecordMap">
        SELECT
          AUTH_ACCOUNT_LOCK_HISTORY_ID,
          AUTH_USER_ID,
          LOCKED_FLG,
          OCCURRED_AT,
          REASON,
          OPERATED_BY,
          CREATED_AT,
          CREATED_BY
        FROM AUTH_ACCOUNT_LOCK_HISTORY
        WHERE AUTH_USER_ID = #{authUserId}
        ORDER BY OCCURRED_AT DESC
    </select>

</mapper>
```

---

## 4. Application: `LoginProcessSharedServiceImpl` の修正ポイント

### 4-1. ロック状態取得

`onLoginFailure` の中の「現在ロック中か確認」部分を、`AccountLockEvents` 経由に変更します。

```java
import com.myou.ec.ecsite.domain.auth.model.AccountLockEvents;
import com.myou.ec.ecsite.domain.auth.model.AccountLockEvent;
import com.myou.ec.ecsite.domain.auth.model.value.LockStatus;
```

```java
AuthUser user = optUser.get();
AuthUserId userId = user.id();
if (userId == null) {
    return LoginFailureType.BAD_CREDENTIALS;
}

LocalDateTime now = LocalDateTime.now();

// ◆ ロックイベント一覧を取得して、現在ステータスを判定
var events = lockHistoryRepository.findByUserId(userId);
AccountLockEvents lockEvents = AccountLockEvents.of(events);
LockStatus status = lockEvents.currentStatus();

if (status.isLocked()) {
    // ロック中のログインは LOCKED で履歴のみ（失敗カウントには含めない）
    LoginHistory lockedHistory = LoginHistory.locked(
            userId,
            now,
            clientIp,
            userAgent,
            loginId
    );
    loginHistoryRepository.save(lockedHistory);
    return LoginFailureType.LOCKED;
}
```

### 4-2. ロックアウト判定時の boundary（最後の UNLOCK 時刻）

`LockPolicy`（ポリシーパターン版）を使う部分はこうなります：

```java
// 直近の履歴を取得
int limit = 20; // 6回しきい値ならこの程度で十分
var recentHistories = loginHistoryRepository.findRecentByUserId(userId, limit);

// 今回の FAIL を先頭に付ける
LoginHistory failHistory = LoginHistory.fail(
        userId,
        now,
        clientIp,
        userAgent,
        loginId
);
var allHistories = new java.util.ArrayList<LoginHistory>(recentHistories.size() + 1);
allHistories.add(failHistory);
allHistories.addAll(recentHistories);

LoginHistories loginHistories = LoginHistories.of(allHistories);

// ◆ AccountLockEvents から「最後の UNLOCK 時刻」をもらう
LocalDateTime lastUnlockAt = lockEvents.lastUnlockAt().orElse(null);

// LockPolicy でロックアウト判定
boolean shouldLockout = lockPolicy.isLockout(loginHistories, lastUnlockAt);

// FAIL 履歴を保存
loginHistoryRepository.save(failHistory);

if (shouldLockout) {
    AccountLockEvent lockEvent = AccountLockEvent.lock(
            userId,
            now,
            "LOGIN_FAIL_THRESHOLD",
            loginId
    );
    lockHistoryRepository.save(lockEvent);
    return LoginFailureType.LOCKED;
} else {
    return LoginFailureType.BAD_CREDENTIALS;
}
```

---

## 5. Application: `AuthUserDetailsService` の修正

`UserDetailsService` で `getLockStatus` を呼んでいた部分も、
`AccountLockEvents` に置き換えます。

```java
import com.myou.ec.ecsite.domain.auth.model.AccountLockEvents;
import com.myou.ec.ecsite.domain.auth.model.AccountLockEvent;
import com.myou.ec.ecsite.domain.auth.model.value.LockStatus;
```

```java
AuthUserId userId = user.id();
boolean locked = false;
if (userId != null) {
    var events = lockHistoryRepository.findByUserId(userId);
    AccountLockEvents lockEvents = AccountLockEvents.of(events);
    locked = lockEvents.currentStatus() == LockStatus.LOCKED;
}
```

---

## まとめ

リファクタ後の構造はこうなります：

* **AccountLockEvent**

    * lock/unlock のイベント 1 件
    * `toLockStatus()` でイベント → LockStatus を表現
* **AccountLockEvents（新規）**

    * イベントのファーストクラスコレクション
    * 「現在のロック状態」「最後の UNLOCK 時刻」を提供
* **AuthAccountLockHistoryRepository**

    * ただイベント一覧を返すだけ（ロジックは持たない）
* **LoginProcessSharedServiceImpl / AuthUserDetailsService**

    * Repository からイベントを取得 → AccountLockEvents に渡して判定

これで、

* 永続化（Repository）
* 状態判定（AccountLockEvents）
* ポリシー（LockPolicy + LoginHistories）

がきれいに分離されて、かなり DDD っぽい構成になっています ✨

このあともし「AccountLockEvents 用のテストコード例」も欲しければ、JUnit のサンプルも出せます。
