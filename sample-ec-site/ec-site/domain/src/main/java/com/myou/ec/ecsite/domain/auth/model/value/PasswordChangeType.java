package com.myou.ec.ecsite.domain.auth.model.value;

/**
 * パスワード変更の種類。
 */
public enum PasswordChangeType {
    INITIAL_REGISTER,   // 初回登録
    ADMIN_RESET,        // 管理者による初期化
    USER_CHANGE         // ユーザ自身による変更
}
