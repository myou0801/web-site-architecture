package com.myou.ec.ecsite.infrastructure.auth.repository;

import com.myou.ec.ecsite.domain.auth.model.PasswordHistory;
import com.myou.ec.ecsite.domain.auth.model.value.AuthUserId;
import com.myou.ec.ecsite.domain.auth.repository.AuthPasswordHistoryRepository;
import com.myou.ec.ecsite.infrastructure.auth.mapper.AuthPasswordHistoryMapper;
import com.myou.ec.ecsite.infrastructure.auth.record.AuthPasswordHistoryRecord;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class AuthPasswordHistoryRepositoryImpl implements AuthPasswordHistoryRepository {

    private final AuthPasswordHistoryMapper mapper;

    public AuthPasswordHistoryRepositoryImpl(AuthPasswordHistoryMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void save(PasswordHistory history) {
        AuthPasswordHistoryRecord record = AuthPasswordHistoryRecord.fromDomain(history);
        mapper.insert(record);
    }

    @Override
    public List<PasswordHistory> findRecentByUserId(AuthUserId userId, int limit) {
        return mapper.findRecentByUserId(userId.value(), limit).stream()
                .map(AuthPasswordHistoryRecord::toDomain)
                .toList();
    }

    @Override
    public Optional<PasswordHistory> findLastByUserId(AuthUserId userId) {
        AuthPasswordHistoryRecord record = mapper.findLastByUserId(userId.value());
        return Optional.ofNullable(record).map(AuthPasswordHistoryRecord::toDomain);
    }
}
