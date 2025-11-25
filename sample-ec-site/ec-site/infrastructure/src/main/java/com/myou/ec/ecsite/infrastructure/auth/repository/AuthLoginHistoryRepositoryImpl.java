package com.myou.ec.ecsite.infrastructure.auth.repository;

import com.myou.ec.ecsite.domain.auth.model.LoginHistories;
import com.myou.ec.ecsite.domain.auth.model.LoginHistory;
import com.myou.ec.ecsite.domain.auth.model.value.AuthUserId;
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
    public void save(LoginHistory history) {
        AuthLoginHistoryRecord record = AuthLoginHistoryRecord.fromDomain(history);
        mapper.insert(record);
    }

    @Override
    public LoginHistories findRecentByUserId(AuthUserId userId, int limit) {
        List<LoginHistory> histories =  mapper.findRecentByUserId(userId.value(), limit).stream()
                .map(AuthLoginHistoryRecord::toDomain)
                .toList();
        return new LoginHistories(histories);
    }

    @Override
    public Optional<LoginHistory> findPreviousSuccessLoginAt(AuthUserId userId) {
        return Optional.ofNullable(mapper.findPreviousSuccessLoginAt(userId.value()).toDomain());
    }

}
