package com.myou.ec.ecsite.infrastructure.auth.repository;

import com.myou.ec.ecsite.domain.auth.model.AuthAccount;
import com.myou.ec.ecsite.domain.auth.model.value.AuthAccountId;
import com.myou.ec.ecsite.domain.auth.model.value.Operator; // Import Operator
import com.myou.ec.ecsite.domain.auth.model.value.UserId;
import com.myou.ec.ecsite.domain.auth.repository.AuthAccountRepository;
import com.myou.ec.ecsite.infrastructure.auth.mapper.AuthAccountMapper;
import com.myou.ec.ecsite.infrastructure.auth.record.AuthAccountRecord;
import org.springframework.stereotype.Repository;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public class AuthAccountRepositoryImpl implements AuthAccountRepository {

    private final AuthAccountMapper userMapper;
    private final Clock clock;

    public AuthAccountRepositoryImpl(AuthAccountMapper userMapper, Clock clock) {
        this.userMapper = userMapper;
        this.clock = clock;
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
    public void save(AuthAccount user, Operator operator) { // Use Operator
        LocalDateTime now = LocalDateTime.now(clock);
        if (user.id() == null) {
            // Insert
            var record = new AuthAccountRecord(
                    null,
                    user.userId().value(),
                    user.passwordHash().value(),
                    user.accountStatus().name(),
                    null, // createdAt is handled by DB
                    operator.value(), // Use operator.value()
                    now,
                    operator.value() // Use operator.value()
            );
            userMapper.insert(record);
        } else {
            // Update
            AuthAccountRecord currentRecord = userMapper.selectByAccountId(user.id().value());
            if (currentRecord == null) {
                throw new IllegalStateException("Attempted to update a non-existent account: " + user.id());
            }

            var record = new AuthAccountRecord(
                    user.id().value(),
                    user.userId().value(),
                    user.passwordHash().value(),
                    user.accountStatus().name(),
                    currentRecord.createdAt(), // preserve original
                    currentRecord.createdBy(), // preserve original
                    now,
                    operator.value() // Use operator.value()
            );
            userMapper.update(record);
        }
    }
}
