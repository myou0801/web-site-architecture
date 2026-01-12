package com.myou.ec.ecsite.application.auth.sharedservice;


import com.myou.ec.ecsite.application.auth.provider.CurrentUserProvider;
import com.myou.ec.ecsite.domain.auth.model.AccountExpiryEvent;
import com.myou.ec.ecsite.domain.auth.model.AccountExpiryEvents;
import com.myou.ec.ecsite.domain.auth.model.LoginHistory;
import com.myou.ec.ecsite.domain.auth.model.value.AuthAccountId;
import com.myou.ec.ecsite.domain.auth.model.value.Operator;
import com.myou.ec.ecsite.domain.auth.policy.AccountExpiryPolicy;
import com.myou.ec.ecsite.domain.auth.repository.AuthAccountExpiryHistoryRepository;
import com.myou.ec.ecsite.domain.auth.repository.AuthLoginHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

@Service
public class AccountExpirySharedServiceImpl implements AccountExpirySharedService {

    public static final String REASON_INACTIVE_90D = "INACTIVE_90D";
    public static final String REASON_ADMIN_ENABLE = "ADMIN_ENABLE";

    private final AuthAccountExpiryHistoryRepository expiryHistoryRepository;
    private final AuthLoginHistoryRepository authLoginHistoryRepository;
    private final AccountExpiryPolicy policy;
    private final CurrentUserProvider currentUserProvider;
    private final Clock clock;

    public AccountExpirySharedServiceImpl(
            AuthAccountExpiryHistoryRepository expiryHistoryRepository,
            AuthLoginHistoryRepository authLoginHistoryRepository,
            AccountExpiryPolicy policy, CurrentUserProvider currentUserProvider, Clock clock
    ) {
        this.expiryHistoryRepository = requireNonNull(expiryHistoryRepository);
        this.authLoginHistoryRepository = requireNonNull(authLoginHistoryRepository);
        this.policy = requireNonNull(policy);
        this.currentUserProvider = currentUserProvider;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    @Override
    public boolean isExpired(AuthAccountId accountId) {
        AccountExpiryEvents events = expiryHistoryRepository.findByAccountId(accountId);
        if (events.isExpired()) {
            return true;
        }

        Optional<LocalDateTime> lastSuccessAt = authLoginHistoryRepository.findLatestSuccessByAccountId(accountId).map(LoginHistory::loginAt);
        Optional<LocalDateTime> lastUnexpireAt = events.lastUnexpireAt();
        Optional<LocalDateTime> baseAt = max(lastSuccessAt, lastUnexpireAt);

        if (baseAt.isEmpty()) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now(clock);
        return now.isAfter(baseAt.get().plus(policy.expiryDuration()));
    }

    @Transactional
    @Override
    public void expireIfNeeded(AuthAccountId accountId) {
        if (!isExpired(accountId)) {
            return;
        }
        
        // 既にEXPIRED状態なら何もしない（isExpiredは計算上の期限切れも含むため、DB上の状態も確認する）
        AccountExpiryEvents events = expiryHistoryRepository.findByAccountId(accountId);
        if (events.isExpired()) {
            return;
        }

        Operator operator = currentUserProvider.currentOrSystem();
        LocalDateTime now = LocalDateTime.now(clock);

        var ev = AccountExpiryEvent.expired(
                accountId,
                REASON_INACTIVE_90D,
                now,
                operator
        );
        expiryHistoryRepository.save(ev, operator);
    }

    @Transactional
    @Override
    public void unexpireIfExpired(AuthAccountId accountId) { // Change LoginId to Operator
        var events = expiryHistoryRepository.findByAccountId(accountId);
        if (!events.isExpired()) {
            return;
        }

        Operator operator = currentUserProvider.currentOrSystem();

        LocalDateTime now = LocalDateTime.now(clock);
        var ev = AccountExpiryEvent.unexpired(
                accountId,
                REASON_ADMIN_ENABLE,
                now,
                operator
            );
        expiryHistoryRepository.save(ev, operator); // Pass Operator directly
    }

    private static Optional<LocalDateTime> max(Optional<LocalDateTime> a, Optional<LocalDateTime> b) {
        if (a.isEmpty()) return b;
        if (b.isEmpty()) return a ;

        return Optional.of(a.get().isAfter(b.get()) ? a.get() : b.get());
    }
}
