package com.myou.ec.ecsite.application.auth.sharedservice;

public enum PasswordChangeRequirementType {
    NONE,               // パスワード強制変更ではない(任意)
    EXPIRED,            // 有効期限切れ
    ADMIN_RESET,        // パスワード初期化
    INITIAL_REGISTER;   // 初回ログイン

    public boolean requiresPasswordChange() {
        return this != PasswordChangeRequirementType.NONE;
    }

}
