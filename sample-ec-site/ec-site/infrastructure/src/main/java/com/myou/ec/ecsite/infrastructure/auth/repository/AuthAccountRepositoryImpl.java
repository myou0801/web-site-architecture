package com.myou.ec.ecsite.infrastructure.auth.repository;

import com.myou.ec.ecsite.domain.auth.model.AuthAccount;
import com.myou.ec.ecsite.domain.auth.model.value.AuthAccountId;
import com.myou.ec.ecsite.domain.auth.model.value.RoleCode;
import com.myou.ec.ecsite.domain.auth.model.value.UserId;
import com.myou.ec.ecsite.domain.auth.repository.AuthAccountRepository;
import com.myou.ec.ecsite.domain.auth.repository.AuthRoleRepository;
import com.myou.ec.ecsite.infrastructure.auth.mapper.AuthAccountMapper;
import com.myou.ec.ecsite.infrastructure.auth.record.AuthAccountRecord;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class AuthAccountRepositoryImpl implements AuthAccountRepository {

    private final AuthAccountMapper userMapper;
    private final AuthRoleRepository authRoleRepository;

    public AuthAccountRepositoryImpl(AuthAccountMapper userMapper,
                                  AuthRoleRepository authRoleRepository) {
        this.userMapper = userMapper;
        this.authRoleRepository = authRoleRepository;
    }

    @Override
    public Optional<AuthAccount> findById(AuthAccountId id) {
        AuthAccountRecord record = userMapper.selectByAccountId(id.value());
        if (record == null) {
            return Optional.empty();
        }
        List<RoleCode> roles = authRoleRepository.findRoleCodesByAccountId(id);
        return Optional.of(record.toDomain(roles));
    }

    @Override
    public Optional<AuthAccount> findByUserId(UserId userId) {
        AuthAccountRecord record = userMapper.selectByUserId(userId.value());
        if (record == null) {
            return Optional.empty();
        }
        AuthAccountId accountId = new AuthAccountId(record.authAccountId());
        List<RoleCode> roles = authRoleRepository.findRoleCodesByAccountId(accountId);
        return Optional.of(record.toDomain(roles));
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
