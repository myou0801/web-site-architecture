package com.myou.ec.ecsite.infrastructure.auth.repository;

import com.myou.ec.ecsite.domain.auth.model.PasswordHistory;
import com.myou.ec.ecsite.domain.auth.model.value.AuthAccountId;
import com.myou.ec.ecsite.domain.auth.model.value.UserId;
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
    public void save(PasswordHistory history, UserId operator) {
        AuthPasswordHistoryRecord record = AuthPasswordHistoryRecord.fromDomain(history, operator);
        mapper.insert(record);
    }

    @Override
    public List<PasswordHistory> findRecentByAccountId(AuthAccountId accountId, int limit) {
        return mapper.selectRecentByAccountId(accountId.value(), limit).stream()
                .map(r -> r.toDomain()) // Need to construct domain object without createdAt/createdBy
                .toList();
    }

    @Override
    public Optional<PasswordHistory> findLastByAccountId(AuthAccountId accountId) {
        AuthPasswordHistoryRecord record = mapper.selectLatestByAccountId(accountId.value());
        return Optional.ofNullable(record).map(r -> r.toDomain()); // Need to construct domain object without createdAt/createdBy
    }
}
