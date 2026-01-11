package com.myou.ec.ecsite.application.auth.sharedservice;

import com.myou.ec.ecsite.domain.auth.model.value.AuthAccountId;

public interface AccountExpirySharedService {
    
    boolean isExpired(AuthAccountId accountId);

    void expireIfNeeded(AuthAccountId accountId);
   
    void unexpireIfExpired(AuthAccountId accountId);
}
