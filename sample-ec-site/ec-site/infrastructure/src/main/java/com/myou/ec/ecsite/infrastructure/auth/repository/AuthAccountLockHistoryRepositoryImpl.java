package com.myou.ec.ecsite.infrastructure.auth.repository;

import com.myou.ec.ecsite.domain.auth.model.AccountLockEvent;
import com.myou.ec.ecsite.domain.auth.model.AccountLockEvents;
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
    public AccountLockEvents findByUserId(AuthUserId userId, int limit) {
        List<AccountLockEvent> events = mapper.findByUserId(userId.value(), limit).stream()
                .map(AuthAccountLockHistoryRecord::toDomain)
                .toList();
        return new AccountLockEvents(events);
    }
}
