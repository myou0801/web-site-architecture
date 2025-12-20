package com.myou.ec.ecsite.application.auth.repository;

import com.myou.ec.ecsite.application.auth.dto.AuthAccountDetailDto;
import com.myou.ec.ecsite.application.auth.dto.AuthAccountRoleDto;
import com.myou.ec.ecsite.application.auth.dto.AuthAccountSearchParam;
import com.myou.ec.ecsite.application.auth.dto.AuthAccountSummaryDto;

import java.util.List;
import java.util.Optional;

public interface AuthAccountQueryRepository {

    List<AuthAccountSummaryDto> findAccountSummaries(AuthAccountSearchParam param);

    long countAccountSummaries(AuthAccountSearchParam param);

    Optional<AuthAccountDetailDto> findAccountDetailByUserId(String userId);

    List<AuthAccountRoleDto> findRoleRecordsByAccountIds(List<Long> authAccountIds);

    List<String> findRoleCodesByAccountId(Long authAccountId);
}
