package com.myou.ec.ecsite.domain.auth.repository;

import com.myou.ec.ecsite.domain.auth.model.value.AuthAccountId;
import com.myou.ec.ecsite.domain.auth.model.value.RoleCode;
import com.myou.ec.ecsite.domain.auth.model.value.UserId;

import java.time.LocalDateTime;
import java.util.Set;

public interface AuthAccountRoleRepository {
    Set<RoleCode> findRolesByAccountId(AuthAccountId accountId);

    int addRole(AuthAccountId accountId, RoleCode role, UserId operator);

    int removeRole(AuthAccountId accountId, RoleCode role);

}
