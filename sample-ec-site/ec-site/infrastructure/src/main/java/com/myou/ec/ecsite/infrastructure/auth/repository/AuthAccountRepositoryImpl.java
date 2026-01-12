package com.myou.ec.ecsite.infrastructure.auth.repository;

import com.myou.ec.ecsite.domain.auth.model.AuthAccount;
import com.myou.ec.ecsite.domain.auth.model.value.AuthAccountId;
import com.myou.ec.ecsite.domain.auth.model.value.LoginId;
import com.myou.ec.ecsite.domain.auth.model.value.Operator;
import com.myou.ec.ecsite.domain.auth.repository.AuthAccountRepository;
import com.myou.ec.ecsite.infrastructure.auth.mapper.AuthAccountMapper;
import com.myou.ec.ecsite.infrastructure.auth.record.AuthAccountRecord;
import org.springframework.stereotype.Repository;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public class AuthAccountRepositoryImpl implements AuthAccountRepository {

    private final AuthAccountMapper authAccountMapper;
    private final Clock clock;

    public AuthAccountRepositoryImpl(AuthAccountMapper authAccountMapper, Clock clock) {
        this.authAccountMapper = authAccountMapper;
        this.clock = clock;
    }

    @Override
    public Optional<AuthAccount> findById(AuthAccountId id) {
        AuthAccountRecord record = authAccountMapper.selectByAccountId(id.value());
        if (record == null) {
            return Optional.empty();
        }
        return Optional.of(record.toDomain());
    }

    @Override
    public Optional<AuthAccount> findByLoginId(LoginId loginId) {
        AuthAccountRecord record = authAccountMapper.selectByLoginId(loginId.value());
        if (record == null) {
            return Optional.empty();
        }
        return Optional.of(record.toDomain());
    }

    @Override
    public void save(AuthAccount authAccount, Operator operator) { // Use Operator
        LocalDateTime now = LocalDateTime.now(clock);
        if (authAccount.id() == null) {
            // Insert
            var record = new AuthAccountRecord(
                    null,
                    authAccount.loginId().value(),
                    authAccount.passwordHash().value(),
                    authAccount.accountStatus().name(),
                    null, // createdAt is handled by DB
                    operator.value(), // Use operator.value()
                    now,
                    operator.value() // Use operator.value()
            );
            authAccountMapper.insert(record);
        } else {
            // Update
            AuthAccountRecord currentRecord = authAccountMapper.selectByAccountId(authAccount.id().value());
            if (currentRecord == null) {
                throw new IllegalStateException("Attempted to update a non-existent account: " + authAccount.id());
            }

            var record = new AuthAccountRecord(
                    authAccount.id().value(),
                    authAccount.loginId().value(),
                    authAccount.passwordHash().value(),
                    authAccount.accountStatus().name(),
                    currentRecord.createdAt(), // preserve original
                    currentRecord.createdBy(), // preserve original
                    now,
                    operator.value() // Use operator.value()
            );
            authAccountMapper.update(record);
        }
    }
}
