package com.myou.ec.ecsite.infrastructure.auth.repository;

import com.myou.ec.ecsite.domain.auth.model.value.AuthAccountId;
import com.myou.ec.ecsite.domain.auth.model.value.RoleCode;
import com.myou.ec.ecsite.domain.auth.model.value.UserId;
import com.myou.ec.ecsite.domain.auth.repository.AuthAccountRoleRepository;
import com.myou.ec.ecsite.infrastructure.auth.mapper.AuthAccountRoleMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;


@Repository
public class AuthAccountRoleRepositoryImpl implements AuthAccountRoleRepository {

    private final AuthAccountRoleMapper authAccountRoleMapper;

    public AuthAccountRoleRepositoryImpl(AuthAccountRoleMapper authAccountRoleMapper) {
        this.authAccountRoleMapper = Objects.requireNonNull(authAccountRoleMapper, "authAccountRoleMapper");
    }

    @Override
    public Set<RoleCode> findRolesByAccountId(AuthAccountId accountId) {
        return authAccountRoleMapper.selectRoleCodesByAccountId(accountId.value())
                .stream()
                .map(RoleCode::new)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public int addRole(AuthAccountId accountId, RoleCode role, UserId operator) {
        return authAccountRoleMapper.insert(accountId.value(), role.value(), operator.value());
    }

    @Override
    public int removeRole(AuthAccountId accountId, RoleCode role) {
        return authAccountRoleMapper.delete(accountId.value(), role.value());
    }
}
