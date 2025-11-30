package com.myou.ec.ecsite.infrastructure.auth.repository;

import com.myou.ec.ecsite.domain.auth.model.AuthAccount;
import com.myou.ec.ecsite.domain.auth.model.value.AuthAccountId;
import com.myou.ec.ecsite.domain.auth.model.value.UserId;
import com.myou.ec.ecsite.domain.auth.repository.AuthAccountRepository;
import com.myou.ec.ecsite.infrastructure.auth.mapper.AuthAccountMapper;
import com.myou.ec.ecsite.infrastructure.auth.record.AuthAccountRecord;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class AuthAccountRepositoryImpl implements AuthAccountRepository {

    private final AuthAccountMapper userMapper;

    public AuthAccountRepositoryImpl(AuthAccountMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public Optional<AuthAccount> findById(AuthAccountId id) {
        AuthAccountRecord record = userMapper.selectByAccountId(id.value());
        if (record == null) {
            return Optional.empty();
        }
        return Optional.of(record.toDomain());
    }

    @Override
    public Optional<AuthAccount> findByUserId(UserId userId) {
        AuthAccountRecord record = userMapper.selectByUserId(userId.value());
        if (record == null) {
            return Optional.empty();
        }
        return Optional.of(record.toDomain());
    }

    @Override
    public void save(AuthAccount user) {
        AuthAccountRecord record = AuthAccountRecord.fromDomain(user);
        if (user.id() == null) {
            userMapper.insert(record);
        } else {
            userMapper.update(record);
        }
    }

}
