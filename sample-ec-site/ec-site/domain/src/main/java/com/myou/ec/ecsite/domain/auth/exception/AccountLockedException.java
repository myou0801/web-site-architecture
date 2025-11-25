package com.myou.ec.ecsite.domain.auth.exception;

/**
 * ロック中アカウントに対するログインなどの例外。
 */
public class AccountLockedException extends AuthDomainException {

    public AccountLockedException() {
        super("アカウントがロックされています。");
    }
}
