package com.myou.ec.ecsite.infrastructure.auth.repository;


import com.myou.ec.ecsite.domain.auth.model.AccountExpiryEvent;
import com.myou.ec.ecsite.domain.auth.model.AccountExpiryEvents;
import com.myou.ec.ecsite.domain.auth.model.value.AuthAccountId;
import com.myou.ec.ecsite.domain.auth.model.value.Operator;
import com.myou.ec.ecsite.domain.auth.repository.AuthAccountExpiryHistoryRepository;
import com.myou.ec.ecsite.infrastructure.auth.mapper.AuthAccountExpiryHistoryMapper;
import com.myou.ec.ecsite.infrastructure.auth.record.AccountExpiryHistoryRecord;
import org.springframework.stereotype.Repository;

import java.time.Clock;

import static java.util.Objects.requireNonNull;

@Repository
public class AuthAccountExpiryHistoryRepositoryImpl implements AuthAccountExpiryHistoryRepository {

    private final AuthAccountExpiryHistoryMapper mapper;
    private final Clock clock; // Inject Clock

    public AuthAccountExpiryHistoryRepositoryImpl(AuthAccountExpiryHistoryMapper mapper, Clock clock) {
        this.mapper = requireNonNull(mapper);
        this.clock = clock;
    }

    @Override
    public AccountExpiryEvents findByAccountId(AuthAccountId accountId) {
        var records = mapper.selectByAccountId(accountId.value());
        var events = records.stream().map(r -> r.toDomain()).toList();
        return AccountExpiryEvents.of(events);
    }

    @Override
    public void save(AccountExpiryEvent event, Operator operator) {
        mapper.insert(AccountExpiryHistoryRecord.fromDomain(event, operator));
    }
}

