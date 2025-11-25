package com.myou.ec.ecsite.domain.auth.model;

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

    public AccountLockEvents(List<AccountLockEvent> values) {
        this.values = Objects.requireNonNull(values, "values must not be null");
    }


    public boolean isLocked() {
        return values.stream()
                .sorted(Comparator.comparing(AccountLockEvent::occurredAt).reversed())
                .findFirst()
                .map(AccountLockEvent::locked)
                .orElse(false);
    }


    /**
     * 最後（最新）のロック解除イベントの時刻。
     * - locked=false のイベントのうち、最も新しい occurredAt。
     */
    public Optional<LocalDateTime> lastUnlockAt() {
        return values.stream()
                .sorted(Comparator.comparing(AccountLockEvent::occurredAt).reversed())
                .filter(event -> !event.locked())
                .findFirst()
                .map(AccountLockEvent::occurredAt);
    }

    public List<AccountLockEvent> asList() {
        return values;
    }

}
