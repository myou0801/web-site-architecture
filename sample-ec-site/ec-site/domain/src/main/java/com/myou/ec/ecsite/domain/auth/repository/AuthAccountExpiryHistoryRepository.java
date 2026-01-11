package com.myou.ec.ecsite.domain.auth.repository;


import com.myou.ec.ecsite.domain.auth.model.AccountExpiryEvent;
import com.myou.ec.ecsite.domain.auth.model.AccountExpiryEvents;
import com.myou.ec.ecsite.domain.auth.model.value.AuthAccountId;
import com.myou.ec.ecsite.domain.auth.model.value.Operator;

public interface AuthAccountExpiryHistoryRepository {
    AccountExpiryEvents findByAccountId(AuthAccountId accountId);

    void save(AccountExpiryEvent event, Operator operator);
}
