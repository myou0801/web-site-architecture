package com.myou.ec.ecsite.domain.auth.policy;

import java.time.Duration;


public class AccountExpiryPolicy {

    private final long expiryDays;

    public AccountExpiryPolicy(long expiryDays) {
        this.expiryDays = expiryDays;
    }

    public Duration expiryDuration() {
        return Duration.ofDays(expiryDays);
    }

    public long expiryDays() {
        return expiryDays;
    }
}
