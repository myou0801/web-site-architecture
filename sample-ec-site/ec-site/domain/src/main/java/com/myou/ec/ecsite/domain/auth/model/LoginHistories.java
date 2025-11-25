package com.myou.ec.ecsite.domain.auth.model;

import com.myou.ec.ecsite.domain.auth.model.value.LoginResult;

import java.time.LocalDateTime;
import java.util.*;

/**
 * LoginHistory のファーストクラスコレクション。
 *
 * - リストの順序は loginAt 降順（新しいものが先頭）に正規化する。
 * - 連続失敗回数のカウント
 * - LockPolicy に基づくロックアウト判定
 * などの振る舞いをここに集約する。
 */
public class LoginHistories {

    private final List<LoginHistory> histories;

    public LoginHistories(List<LoginHistory> histories) {
        this.histories = Objects.requireNonNull(histories, "histories must not be null");
    }

    public LoginHistories add(LoginHistory history) {
        List<LoginHistory> newHistories = new ArrayList<>(histories);
        newHistories.add(history);
        return new LoginHistories(newHistories);
    }


    /**
     * 前回ログイン日時（今回を除く直近 SUCCESS）を返す。
     * 履歴が1件以下、またはSUCCESSがない場合は empty。
     */
    public Optional<LocalDateTime> findPreviousSuccessLoginAt() {
        LocalDateTime firstSuccess = null;
        for (LoginHistory history : histories) {
            if (history.result() == LoginResult.SUCCESS) {
                if (firstSuccess == null) {
                    firstSuccess = history.loginAt();
                } else {
                    // 2件目の SUCCESS が「前回ログイン」
                    return Optional.of(history.loginAt());
                }
            }
        }
        return Optional.empty();
    }

    /**
     * 連続失敗回数をカウントする。
     *
     * - 最新の履歴から順に見ていき、FAIL が続く限りカウント
     * - SUCCESS/LOCKED/DISABLED など FAIL 以外が出た時点で打ち切り
     * - boundaryExclusive（最後の UNLOCK など）が渡された場合、
     *   それより前の履歴はカウント対象外
     */
    public int countConsecutiveFailuresSince(LocalDateTime boundaryExclusive) {
        int count = 0;

        //降順にソート
        final List<LoginHistory> h = histories.stream()
                .sorted(Comparator.comparing(LoginHistory::loginAt).reversed()).toList();

        for (LoginHistory history : h) {
            if (boundaryExclusive != null && history.loginAt().isBefore(boundaryExclusive)) {
                break;
            }

            LoginResult result = history.result();
            if (result == LoginResult.FAIL) {
                count++;
                continue;
            }

            // 失敗以外が出たら連続失敗はそこで途切れる
            break;
        }
        return count;
    }

}
