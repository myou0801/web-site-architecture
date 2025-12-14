package com.myou.ec.ecsite.domain.auth.repository;


import com.myou.ec.ecsite.domain.auth.model.AccountExpiryEvent;
import com.myou.ec.ecsite.domain.auth.model.AccountExpiryEvents;
import com.myou.ec.ecsite.domain.auth.model.value.AuthAccountId;

public interface AuthAccountExpiryHistoryRepository {
    AccountExpiryEvents findByAccountId(AuthAccountId accountId);
    void save(AccountExpiryEvent event, String createdBy);
}
