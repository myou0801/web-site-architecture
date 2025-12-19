package com.myou.ec.ecsite.infrastructure.auth.repository;

import com.myou.ec.ecsite.domain.auth.model.AuthAccountStatusHistory;
import com.myou.ec.ecsite.domain.auth.model.value.UserId;
import com.myou.ec.ecsite.domain.auth.repository.AuthAccountStatusHistoryRepository;
import com.myou.ec.ecsite.infrastructure.auth.mapper.AuthAccountStatusHistoryMapper;
import com.myou.ec.ecsite.infrastructure.auth.record.AuthAccountStatusHistoryRecord;
import org.springframework.stereotype.Repository;

import java.util.Objects;

@Repository
public class AuthAccountStatusHistoryRepositoryImpl implements AuthAccountStatusHistoryRepository {

    private final AuthAccountStatusHistoryMapper mapper;

    public AuthAccountStatusHistoryRepositoryImpl(AuthAccountStatusHistoryMapper mapper) {
        this.mapper = Objects.requireNonNull(mapper);
    }

    @Override
    public void save(AuthAccountStatusHistory history, UserId operator) {
        AuthAccountStatusHistoryRecord record = AuthAccountStatusHistoryRecord.fromDomain(history, operator);
        mapper.insert(record);
    }
}
