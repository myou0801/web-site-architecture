package com.myou.ec.ecsite.application.auth.sharedservice;

/**
 * ログイン成功時の結果。
 * 今は「パスワード変更が必須かどうか」だけを返す。
 */
public record LoginSuccessResult(
        boolean passwordChangeRequired
) {
}
