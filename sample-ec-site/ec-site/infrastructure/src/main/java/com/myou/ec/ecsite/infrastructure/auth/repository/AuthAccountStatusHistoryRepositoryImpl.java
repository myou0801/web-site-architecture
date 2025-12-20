package com.myou.ec.ecsite.infrastructure.auth.repository;

import com.myou.ec.ecsite.domain.auth.model.AuthAccountStatusHistory;
import com.myou.ec.ecsite.domain.auth.model.value.Operator;
import com.myou.ec.ecsite.domain.auth.repository.AuthAccountStatusHistoryRepository;
import com.myou.ec.ecsite.infrastructure.auth.mapper.AuthAccountStatusHistoryMapper;
import com.myou.ec.ecsite.infrastructure.auth.record.AuthAccountStatusHistoryRecord;
import org.springframework.stereotype.Repository;

import java.time.Clock;
import java.util.Objects;

@Repository
public class AuthAccountStatusHistoryRepositoryImpl implements AuthAccountStatusHistoryRepository {

    private final AuthAccountStatusHistoryMapper mapper;
    private final Clock clock; // Inject Clock

    public AuthAccountStatusHistoryRepositoryImpl(AuthAccountStatusHistoryMapper mapper, Clock clock) {
        this.mapper = Objects.requireNonNull(mapper);
        this.clock = clock;
    }

    @Override
    public void save(AuthAccountStatusHistory history, Operator operator) { // Use Operator
        AuthAccountStatusHistoryRecord record = AuthAccountStatusHistoryRecord.fromDomain(history, operator); // Pass Operator directly
        mapper.insert(record);
    }
}
