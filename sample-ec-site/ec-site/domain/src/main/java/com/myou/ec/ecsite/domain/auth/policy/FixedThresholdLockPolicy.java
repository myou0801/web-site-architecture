package com.myou.ec.ecsite.domain.auth.policy;

import com.myou.ec.ecsite.domain.auth.model.LoginHistories;

import java.time.LocalDateTime;

/**
 * シンプルな「N回連続失敗でロック」ポリシー。
 */
public record FixedThresholdLockPolicy() implements LockPolicy {

    // 失敗回数
    private static final int FAIL_THRESHOLD = 6;

    @Override
    public boolean isLockout(LoginHistories histories, LocalDateTime boundaryExclusive) {
        int count = histories.countConsecutiveFailuresSince(boundaryExclusive);
        return count >= FAIL_THRESHOLD;
    }
}