package com.myou.ec.ecsite.infrastructure.auth.repository;

import com.myou.ec.ecsite.domain.auth.model.LoginHistories;
import com.myou.ec.ecsite.domain.auth.model.LoginHistory;
import com.myou.ec.ecsite.domain.auth.model.value.AuthAccountId;
import com.myou.ec.ecsite.domain.auth.model.value.UserId;
import com.myou.ec.ecsite.domain.auth.repository.AuthLoginHistoryRepository;
import com.myou.ec.ecsite.infrastructure.auth.mapper.AuthLoginHistoryMapper;
import com.myou.ec.ecsite.infrastructure.auth.record.AuthLoginHistoryRecord;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class AuthLoginHistoryRepositoryImpl implements AuthLoginHistoryRepository {

    private final AuthLoginHistoryMapper mapper;

    public AuthLoginHistoryRepositoryImpl(AuthLoginHistoryMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void save(LoginHistory history, UserId operator) {
        AuthLoginHistoryRecord record = AuthLoginHistoryRecord.fromDomain(history, operator);
        mapper.insert(record);
    }

    @Override
    public LoginHistories findRecentByAccountId(AuthAccountId accountId, int limit) {
        List<LoginHistory> histories =  mapper.selectRecentByAccountId(accountId.value(), limit).stream()
                .map(AuthLoginHistoryRecord::toDomain)
                .toList();
        return new LoginHistories(histories);
    }

    @Override
    public Optional<LoginHistory> findLatestSuccessByAccountId(AuthAccountId accountId) {
        return Optional.ofNullable(mapper.selectLatestSuccessByAccountId(accountId.value()))
                .map(AuthLoginHistoryRecord::toDomain);
    }

}
