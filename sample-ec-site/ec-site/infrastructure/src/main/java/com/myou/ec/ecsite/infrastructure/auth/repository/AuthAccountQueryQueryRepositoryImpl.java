package com.myou.ec.ecsite.infrastructure.auth.repository;

import com.myou.ec.ecsite.application.auth.dto.AuthAccountDetailDto;
import com.myou.ec.ecsite.application.auth.dto.AuthAccountRoleDto;
import com.myou.ec.ecsite.application.auth.dto.AuthAccountSearchParam;
import com.myou.ec.ecsite.application.auth.dto.AuthAccountSummaryDto;
import com.myou.ec.ecsite.application.auth.repository.AuthAccountQueryRepository;
import com.myou.ec.ecsite.infrastructure.auth.mapper.AuthAccountQueryMapper;
import com.myou.ec.ecsite.infrastructure.auth.mapper.AuthAccountRoleQueryMapper;
import com.myou.ec.ecsite.infrastructure.auth.record.AuthAccountRoleRecord;
import com.myou.ec.ecsite.infrastructure.auth.record.AuthAccountSummaryRecord;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class AuthAccountQueryQueryRepositoryImpl implements AuthAccountQueryRepository {

    private final AuthAccountQueryMapper accountQueryMapper;
    private final AuthAccountRoleQueryMapper accountRoleQueryMapper;

    public AuthAccountQueryQueryRepositoryImpl(AuthAccountQueryMapper accountQueryMapper, AuthAccountRoleQueryMapper accountRoleQueryMapper) {
        this.accountQueryMapper = accountQueryMapper;
        this.accountRoleQueryMapper = accountRoleQueryMapper;
    }

    @Override
    public List<AuthAccountSummaryDto> findAccountSummaries(AuthAccountSearchParam param) {
        List<AuthAccountSummaryRecord> records = accountQueryMapper.selectSummaries(param);
        return records.stream().map(r -> {
            AuthAccountSummaryDto dto = new AuthAccountSummaryDto();
            dto.authAccountId = r.authAccountId;
            dto.userId = r.userId;
            dto.accountStatus = r.accountStatus;
            dto.locked = r.locked != null && r.locked;
            dto.expired = r.expired != null && r.expired;
            dto.createdAt = r.createdAt;
            dto.updatedAt = r.updatedAt;
            dto.lastLoginAt = r.lastLoginAt;
            return dto;
        }).collect(java.util.stream.Collectors.toList());
    }

    @Override
    public long countAccountSummaries(AuthAccountSearchParam param) {
        return accountQueryMapper.countSummaries(param);
    }

    @Override
    public Optional<AuthAccountDetailDto> findAccountDetailByUserId(String userId) {
        return accountQueryMapper.selectDetailByUserId(userId)
                .map(r -> {
                    AuthAccountDetailDto dto = new AuthAccountDetailDto();
                    dto.authAccountId = r.authAccountId;
                    dto.userId = r.userId;
                    dto.accountStatus = r.accountStatus;
                    dto.locked = r.locked != null && r.locked;
                    dto.expired = r.expired != null && r.expired;
                    dto.createdAt = r.createdAt;
                    dto.updatedAt = r.updatedAt;
                    dto.lastLoginAt = r.lastLoginAt;
                    return dto;
                });
    }

    @Override
    public List<AuthAccountRoleDto> findRoleRecordsByAccountIds(List<Long> authAccountIds) {
        List<AuthAccountRoleRecord> records = accountRoleQueryMapper.selectRoleCodesByAccountIds(authAccountIds);
        return records.stream().map(r -> {
            AuthAccountRoleDto dto = new AuthAccountRoleDto();
            dto.authAccountId = r.authAccountId;
            dto.roleCode = r.roleCode;
            return dto;
        }).collect(java.util.stream.Collectors.toList());
    }

    @Override
    public List<String> findRoleCodesByAccountId(Long authAccountId) {
        return accountRoleQueryMapper.selectRoleCodesByAccountId(authAccountId);
    }
}
