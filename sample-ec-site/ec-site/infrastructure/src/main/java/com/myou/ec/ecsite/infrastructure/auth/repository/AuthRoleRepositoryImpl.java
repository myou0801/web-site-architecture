package com.myou.ec.ecsite.infrastructure.auth.repository;

import com.myou.ec.ecsite.domain.auth.model.AuthRole;
import com.myou.ec.ecsite.domain.auth.model.value.AuthUserId;
import com.myou.ec.ecsite.domain.auth.model.value.RoleCode;
import com.myou.ec.ecsite.domain.auth.repository.AuthRoleRepository;
import com.myou.ec.ecsite.infrastructure.auth.mapper.AuthRoleMapper;
import com.myou.ec.ecsite.infrastructure.auth.record.AuthRoleRecord;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public class AuthRoleRepositoryImpl implements AuthRoleRepository {

    private final AuthRoleMapper mapper;

    public AuthRoleRepositoryImpl(AuthRoleMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<AuthRole> findAll() {
        return mapper.findAll().stream()
                .map(AuthRoleRecord::toDomain)
                .toList();
    }

    @Override
    public List<RoleCode> findRoleCodesByUserId(AuthUserId authUserId) {
        return mapper.findRoleCodesByUserId(authUserId.value()).stream()
                .map(RoleCode::new)
                .toList();
    }

    @Override
    @Transactional
    public void saveUserRoles(AuthUserId authUserId, List<RoleCode> roleCodes) {
        long id = authUserId.value();
        mapper.deleteUserRoles(id);
        if (roleCodes != null && !roleCodes.isEmpty()) {
            for (RoleCode roleCode : roleCodes) {
                mapper.insertUserRole(id, roleCode.value());
            }
        }
    }
}
