package com.myou.ec.ecsite.application.auth.sharedservice;

import com.myou.ec.ecsite.application.auth.dto.AuthAccountDetailDto;
import com.myou.ec.ecsite.application.auth.dto.AuthAccountSearchRequest;
import com.myou.ec.ecsite.application.auth.dto.AuthAccountSummaryDto;
import com.myou.ec.ecsite.application.auth.dto.PageDto;

import java.util.Optional;

public interface AuthAccountQuerySharedService {

    PageDto<AuthAccountSummaryDto> search(AuthAccountSearchRequest request);

    Optional<AuthAccountDetailDto> findByUserId(String userId);
}
