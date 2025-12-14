package com.myou.ec.ecsite.application.auth.sharedservice;

import com.myou.ec.ecsite.domain.auth.model.value.AuthAccountId;
import com.myou.ec.ecsite.domain.auth.model.value.UserId;
import org.springframework.transaction.annotation.Transactional;

public interface AccountExpirySharedService {
    @Transactional
    boolean evaluateAndExpireIfNeeded(AuthAccountId accountId, UserId loginUserId);

    @Transactional
    void unexpireIfExpired(AuthAccountId accountId, UserId operator);
}
