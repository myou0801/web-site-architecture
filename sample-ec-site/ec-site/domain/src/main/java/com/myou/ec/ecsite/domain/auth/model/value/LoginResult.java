package com.myou.ec.ecsite.domain.auth.model.value;

/**
 * ログイン試行の結果種別。
 */
public enum LoginResult {
    SUCCESS,
    FAILURE,
    LOCKED,
    DISABLED,
    EXPIRED
}
