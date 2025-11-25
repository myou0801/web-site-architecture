いいですね、その方向性すごく DDD っぽくなります 👍

やりたいことは：

* `LoginHistory` のリストを「ただの `List`」ではなく **ファーストクラスコレクション**として扱う
* そのコレクションに

  * 「連続失敗回数を数える」
  * 「ロックアウトすべきか（`LockPolicy` に基づく）」
    という**振る舞いを持たせる**

です。

以下の流れで提案します。

1. `LoginHistories` ファーストクラスコレクションクラスを domain に追加
2. `AuthLoginHistoryRepository` から `countConsecutiveFailures...` を削除
3. `LoginProcessSharedServiceImpl` のロック判定ロジックを `LoginHistories` に委譲

---

## 1. LoginHistories（ファーストクラスコレクション）

パッケージ：`com.myou.ec.ecsite.domain.auth.model`

```java
package com.myou.ec.ecsite.domain.auth.model;

import com.myou.ec.ecsite.domain.auth.model.value.LockPolicy;
import com.myou.ec.ecsite.domain.auth.model.value.LoginResult;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * LoginHistory のファーストクラスコレクション。
 *
 * - リストの順序は loginAt 降順（新しいものが先頭）に正規化する。
 * - 連続失敗回数のカウント
 * - LockPolicy に基づくロックアウト判定
 * などの振る舞いをここに集約する。
 */
public class LoginHistories {

    private final List<LoginHistory> values; // loginAt 降順

    private LoginHistories(List<LoginHistory> values) {
        // null チェック + 降順にソートして不変リストにする
        this.values = List.copyOf(
                Objects.requireNonNull(values, "values must not be null")
                        .stream()
                        .sorted(Comparator.comparing(LoginHistory::loginAt).reversed())
                        .toList()
        );
    }

    public static LoginHistories of(List<LoginHistory> values) {
        return new LoginHistories(values);
    }

    /**
     * 連続失敗回数をカウントする。
     *
     * - 最新の履歴から順に見ていき、FAIL が続く限りカウントする
     * - SUCCESS/LOCKED/DISABLED など FAIL 以外が出たところで打ち切る
     * - boundaryExclusive（最後の UNLOCK 時刻など）が渡された場合、
     *   それより前の履歴はカウント対象外とする
     *
     * @param boundaryExclusive この日時より前の履歴は見ない（null の場合は無視）
     * @return 連続失敗回数
     */
    public int countConsecutiveFailuresSince(LocalDateTime boundaryExclusive) {
        int count = 0;

        for (LoginHistory history : values) {
            if (boundaryExclusive != null && history.loginAt().isBefore(boundaryExclusive)) {
                break;
            }

            LoginResult result = history.result();
            if (result == LoginResult.FAIL) {
                count++;
                continue;
            }

            // SUCCESS / LOCKED / DISABLED などが出たら連続失敗はそこで途切れる
            break;
        }
        return count;
    }

    /**
     * LockPolicy に基づいてロックアウトすべきかどうかを判定する。
     */
    public boolean isLockout(LockPolicy lockPolicy, LocalDateTime boundaryExclusive) {
        int consecutiveFails = countConsecutiveFailuresSince(boundaryExclusive);
        return lockPolicy.isOverThreshold(consecutiveFails);
    }

    public List<LoginHistory> asList() {
        return values;
    }
}
```

ポイント：

* 「連続失敗」は **最新から見て FAIL が続くところまで** という定義にしています。
* `boundaryExclusive` に「最後の UNLOCK 時刻」を渡すことで
  「最後の UNLOCK より前の FAIL はカウントしない」ようにできます。

---

## 2. AuthLoginHistoryRepository の整理

すでにこんな感じでしたよね：

```java
public interface AuthLoginHistoryRepository {

    void save(LoginHistory history);

    List<LoginHistory> findRecentByUserId(AuthUserId userId, int limit);

    Optional<LocalDateTime> findPreviousSuccessLoginAt(AuthUserId userId);

    int countConsecutiveFailuresSinceLastSuccessOrUnlock(AuthUserId userId);
}
```

このうち

```java
int countConsecutiveFailuresSinceLastSuccessOrUnlock(AuthUserId userId);
```

は **ドメインロジックをインフラ側に寄せているメソッド** なので、
今回の `LoginHistories` 導入に伴って削除してOKです。

修正案：

```java
package com.myou.ec.ecsite.domain.auth.repository;

import com.myou.ec.ecsite.domain.auth.model.LoginHistory;
import com.myou.ec.ecsite.domain.auth.model.value.AuthUserId;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AuthLoginHistoryRepository {

    void save(LoginHistory history);

    /**
     * 対象ユーザの直近のログイン履歴を loginAt 降順で取得する。
     *
     * @param userId ユーザID
     * @param limit  取得最大件数
     */
    List<LoginHistory> findRecentByUserId(AuthUserId userId, int limit);

    /**
     * 前回ログイン日時（今回を除く直近 SUCCESS）を返す。
     */
    Optional<LocalDateTime> findPreviousSuccessLoginAt(AuthUserId userId);
}
```

* infra 側の `AuthLoginHistoryRepositoryImpl` も
  `countConsecutiveFailuresSinceLastSuccessOrUnlock` の実装を削除してOKです。

---

## 3. LoginProcessSharedServiceImpl の修正

`onLoginFailure` で、以前はリポジトリに丸投げしていた部分を
`LoginHistories` に置き換えます。

### 変更後の `onLoginFailure`

```java
@Override
public LoginFailureType onLoginFailure(String loginIdValue, String clientIp, String userAgent) {
    if (loginIdValue == null || loginIdValue.isBlank()) {
        return LoginFailureType.BAD_CREDENTIALS;
    }

    LoginId loginId = new LoginId(loginIdValue);
    Optional<AuthUser> optUser = authUserRepository.findByLoginId(loginId);

    if (optUser.isEmpty()) {
        // ユーザが存在しない場合は履歴を残さない（情報漏洩防止）
        return LoginFailureType.BAD_CREDENTIALS;
    }

    AuthUser user = optUser.get();
    AuthUserId userId = user.id();
    if (userId == null) {
        return LoginFailureType.BAD_CREDENTIALS;
    }

    LocalDateTime now = LocalDateTime.now();

    // 現在ロック中か確認
    LockStatus status = lockHistoryRepository.getLockStatus(userId);
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

    // ロックされていない場合 → FAIL として履歴を追加する前に
    // 直近の履歴を取得し、今回の FAIL を含めた LoginHistories を組み立てる
    int limit = lockPolicy.failThreshold() * 2; // 十分な件数を取っておけばOK
    var recentHistories = loginHistoryRepository.findRecentByUserId(userId, limit);

    // 今回の FAIL を先頭に付けたリストを作成
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

    // 最後の UNLOCK 時刻（なければ null）
    LocalDateTime lastUnlockAt = lockHistoryRepository.findLatestByUserId(userId)
            .filter(ev -> !ev.locked())              // locked=false のイベントが UNLOCK
            .map(AccountLockEvent::occurredAt)
            .orElse(null);

    boolean shouldLockout = loginHistories.isLockout(lockPolicy, lastUnlockAt);

    // FAIL 履歴を保存
    loginHistoryRepository.save(failHistory);

    if (shouldLockout) {
        // 閾値超え → ロックイベント登録
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
}
```

### ここでの DDD 的なポイント

* **連続失敗の数え方のルール**（どこまでをカウントするか、UNLOCK より前は除外するか）は
  → `LoginHistories` ＋ `LockPolicy` に閉じ込めた
  （＝ドメイン層の責務）
* application 層（`LoginProcessSharedServiceImpl`）は

  * 「ドメインに必要な材料（履歴と最後のUNLOCK時刻）を集めて渡す」
  * 「結果に応じて、ロックイベントを積む／エラー種別を返す」
    というユースケースの編成だけを担当

インフラ側からは

* `findRecentByUserId`
* `findLatestByUserId`（AccountLockEvent）

という「生データ」をもらい、
ロジックはドメインに集約できているので、だいぶ DDD らしい形になっていると思います。

---

## まとめ

やる作業はこの3つです：

1. `LoginHistories` クラスを domain に追加する
2. `AuthLoginHistoryRepository` から
   `countConsecutiveFailuresSinceLastSuccessOrUnlock` を削除
3. `LoginProcessSharedServiceImpl.onLoginFailure` を
   `LoginHistories` + `LockPolicy` + `AccountLockEvent` で判定する形に変更

ここまで入れておくと、

* 連続失敗ロジックの仕様変更（「LOCKED をどう扱うか」「何を境界にするか」など）があっても
  基本的には `LoginHistories` だけ直せば済む
* テストも `LoginHistoriesTest` として純粋なドメインテストが書ける

という状態になるので、かなり気持ちよく DDD っぽくなると思います。

テストコードの例（JUnit）も欲しければ、`LoginHistories` 用のテストも一緒に書きますよ。
