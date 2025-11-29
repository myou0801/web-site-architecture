package com.myou.ec.ecsite.domain.auth.model.policy;

import com.myou.ec.ecsite.domain.auth.exception.PasswordPolicyViolationException;
import com.myou.ec.ecsite.domain.auth.model.value.UserId;

/**
 * パスワードポリシー（ポリシーパターン）。
 *
 * 実装クラスごとに構文チェックや有効期限ルールを切り替えられる。
 */
public interface PasswordPolicy {

    void validatePassword(String newRawPassword, UserId userId)
                                    throws PasswordPolicyViolationException;

//    /**
//     * パスワード構文チェック。
//     *
//     * @param rawPassword 生パスワード
//     * @param loginId     ログインID（ログインIDとの完全一致禁止などの判定用）
//     *
//     * @throws com.myou.ec.ecsite.domain.auth.exception.PasswordPolicyViolationException
//     *         ポリシー違反の場合
//     */
//    void validateSyntax(String rawPassword, LoginId loginId);
//
//    /**
//     * パスワード有効期限切れかどうか。
//     *
//     * @param lastChangedAt 最後にパスワードを変更した日時
//     * @param now           現在日時
//     */
//    boolean isExpired(LocalDateTime lastChangedAt, LocalDateTime now);
//
//    /**
//     * パスワード履歴で「再利用禁止」とする世代数。
//     */
//    int historyGenerationCount();
}
