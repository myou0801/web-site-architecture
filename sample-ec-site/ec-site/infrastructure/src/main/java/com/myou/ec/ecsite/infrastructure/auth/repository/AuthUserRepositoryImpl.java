package com.myou.ec.ecsite.infrastructure.auth.repository;

import com.myou.ec.ecsite.domain.auth.model.AuthUser;
import com.myou.ec.ecsite.domain.auth.model.value.AuthUserId;
import com.myou.ec.ecsite.domain.auth.model.value.LoginId;
import com.myou.ec.ecsite.domain.auth.model.value.RoleCode;
import com.myou.ec.ecsite.domain.auth.repository.AuthRoleRepository;
import com.myou.ec.ecsite.domain.auth.repository.AuthUserRepository;
import com.myou.ec.ecsite.infrastructure.auth.mapper.AuthUserMapper;
import com.myou.ec.ecsite.infrastructure.auth.record.AuthUserRecord;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class AuthUserRepositoryImpl implements AuthUserRepository {

    private final AuthUserMapper userMapper;
    private final AuthRoleRepository authRoleRepository;

    public AuthUserRepositoryImpl(AuthUserMapper userMapper,
                                  AuthRoleRepository authRoleRepository) {
        this.userMapper = userMapper;
        this.authRoleRepository = authRoleRepository;
    }

    @Override
    public Optional<AuthUser> findById(AuthUserId id) {
        AuthUserRecord record = userMapper.findById(id.value());
        if (record == null) {
            return Optional.empty();
        }
        List<RoleCode> roles = authRoleRepository.findRoleCodesByUserId(id);
        return Optional.of(record.toDomain(roles));
    }

    @Override
    public Optional<AuthUser> findByLoginId(LoginId loginId) {
        AuthUserRecord record = userMapper.findByLoginId(loginId.value());
        if (record == null) {
            return Optional.empty();
        }
        AuthUserId userId = new AuthUserId(record.authUserId());
        List<RoleCode> roles = authRoleRepository.findRoleCodesByUserId(userId);
        return Optional.of(record.toDomain(roles));
    }

    @Override
    public void save(AuthUser user) {
        AuthUserRecord record = AuthUserRecord.fromDomain(user);
        if (user.id() == null) {
            userMapper.insert(record);
        } else {
            userMapper.update(record);
        }
    }
}
