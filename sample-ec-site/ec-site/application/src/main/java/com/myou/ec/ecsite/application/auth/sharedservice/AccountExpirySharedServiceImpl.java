package com.myou.ec.ecsite.application.auth.sharedservice;


import com.myou.ec.ecsite.domain.auth.model.AccountExpiryEvent;
import com.myou.ec.ecsite.domain.auth.model.AccountExpiryEvents;
import com.myou.ec.ecsite.domain.auth.model.LoginHistory;
import com.myou.ec.ecsite.domain.auth.model.value.AuthAccountId;
import com.myou.ec.ecsite.domain.auth.model.value.UserId;
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
    private final Clock clock;

    public AccountExpirySharedServiceImpl(
            AuthAccountExpiryHistoryRepository expiryHistoryRepository,
            AuthLoginHistoryRepository authLoginHistoryRepository,
            AccountExpiryPolicy policy, Clock clock
    ) {
        this.expiryHistoryRepository = requireNonNull(expiryHistoryRepository);
        this.authLoginHistoryRepository = requireNonNull(authLoginHistoryRepository);
        this.policy = requireNonNull(policy);
        this.clock = clock;
    }

    @Transactional
    @Override
    public boolean evaluateAndExpireIfNeeded(AuthAccountId accountId, UserId loginUserId) {
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
        boolean expired = now.isAfter(baseAt.get().plus(policy.expiryDuration()));
        if (expired) {
            var ev = AccountExpiryEvent.expired(
                    accountId,
                    REASON_INACTIVE_90D,
                    now,
                    loginUserId
            );
            expiryHistoryRepository.save(ev, loginUserId != null ? loginUserId.value() : "SYSTEM");
            return true;
        }
        return false;
    }

    @Transactional
    @Override
    public void unexpireIfExpired(AuthAccountId accountId, UserId operator) {
        var events = expiryHistoryRepository.findByAccountId(accountId);
        if (!events.isExpired()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now(clock);
        var ev = AccountExpiryEvent.unexpired(
                accountId,
                REASON_ADMIN_ENABLE,
                now,
                operator
        );
        expiryHistoryRepository.save(ev, operator.value());
    }

    private static Optional<LocalDateTime> max(Optional<LocalDateTime> a, Optional<LocalDateTime> b) {
        if (a.isEmpty()) return b;
        if (b.isEmpty()) return a ;

        return Optional.of(a.get().isAfter(b.get()) ? a.get() : b.get());
    }
}
