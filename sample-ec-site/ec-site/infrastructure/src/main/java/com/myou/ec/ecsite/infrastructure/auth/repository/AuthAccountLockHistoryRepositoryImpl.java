package com.myou.ec.ecsite.infrastructure.auth.repository;

import com.myou.ec.ecsite.domain.auth.model.AccountLockEvent;
import com.myou.ec.ecsite.domain.auth.model.AccountLockEvents;
import com.myou.ec.ecsite.domain.auth.model.value.AuthAccountId;
import com.myou.ec.ecsite.domain.auth.model.value.Operator; // Import Operator
import com.myou.ec.ecsite.domain.auth.model.value.UserId;
import com.myou.ec.ecsite.domain.auth.repository.AuthAccountLockHistoryRepository;
import com.myou.ec.ecsite.infrastructure.auth.mapper.AuthAccountLockHistoryMapper;
import com.myou.ec.ecsite.infrastructure.auth.record.AuthAccountLockHistoryRecord;
import org.springframework.stereotype.Repository;

import java.time.Clock;
import java.util.List;

@Repository
public class AuthAccountLockHistoryRepositoryImpl implements AuthAccountLockHistoryRepository {

    private final AuthAccountLockHistoryMapper mapper;
    private final Clock clock; // Inject Clock

    public AuthAccountLockHistoryRepositoryImpl(AuthAccountLockHistoryMapper mapper, Clock clock) {
        this.mapper = mapper;
        this.clock = clock;
    }

    @Override
    public void save(AccountLockEvent event, Operator operator) { // Use Operator
        AuthAccountLockHistoryRecord record = AuthAccountLockHistoryRecord.fromDomain(event, operator); // Pass Operator directly
        mapper.insert(record);
    }

    @Override
    public AccountLockEvents findByAccountId(AuthAccountId accountId, int limit) {
        List<AccountLockEvent> events = mapper.selectRecentByAccountId(accountId.value(), limit).stream()
                .map(r -> r.toDomain())
                .toList();
        return new AccountLockEvents(events);
    }
}
