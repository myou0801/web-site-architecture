package com.myou.ec.ecsite.domain.auth.model;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class AccountExpiryEvents {

    private final List<AccountExpiryEvent> eventsDescending;

    private AccountExpiryEvents(List<AccountExpiryEvent> eventsDescending) {
        this.eventsDescending = List.copyOf(eventsDescending);
    }

    public static AccountExpiryEvents of(List<AccountExpiryEvent> events) {
        if (events == null || events.isEmpty()) {
            return new AccountExpiryEvents(List.of());
        }
        var sorted = events.stream()
                .sorted(Comparator.comparing(AccountExpiryEvent::occurredAt).reversed())
                .toList();
        return new AccountExpiryEvents(sorted);
    }

    public boolean isExpired() {
        return eventsDescending.stream().findFirst().map(AccountExpiryEvent::isExpire).orElse(false);
    }

    public Optional<LocalDateTime> lastUnexpireAt() {
        return eventsDescending.stream()
                .filter(AccountExpiryEvent::isUnexpire)
                .map(AccountExpiryEvent::occurredAt)
                .findFirst();
    }
}
