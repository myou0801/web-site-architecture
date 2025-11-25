package com.myou.ec.ecsite.domain.auth.exception;

/**
 * パスワード再利用禁止（履歴ルール違反）の例外。
 */
public class PasswordReuseNotAllowedException extends AuthDomainException {

    public PasswordReuseNotAllowedException() {
        super("パスワードは直近の履歴と同じ値は利用できません。");
    }
}
