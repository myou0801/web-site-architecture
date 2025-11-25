package com.myou.ec.ecsite.application.auth.sharedservice;


import com.myou.ec.ecsite.domain.auth.exception.AuthDomainException;
import com.myou.ec.ecsite.domain.auth.model.*;
import com.myou.ec.ecsite.domain.auth.model.policy.LockPolicy;
import com.myou.ec.ecsite.domain.auth.model.policy.PasswordPolicy;
import com.myou.ec.ecsite.domain.auth.model.value.AuthUserId;
import com.myou.ec.ecsite.domain.auth.model.value.LoginId;
import com.myou.ec.ecsite.domain.auth.repository.AuthAccountLockHistoryRepository;
import com.myou.ec.ecsite.domain.auth.repository.AuthLoginHistoryRepository;
import com.myou.ec.ecsite.domain.auth.repository.AuthPasswordHistoryRepository;
import com.myou.ec.ecsite.domain.auth.repository.AuthUserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class LoginProcessSharedServiceImpl implements LoginProcessSharedService {

    private final AuthUserRepository authUserRepository;
    private final AuthLoginHistoryRepository loginHistoryRepository;
    private final AuthPasswordHistoryRepository passwordHistoryRepository;
    private final AuthAccountLockHistoryRepository lockHistoryRepository;
    private final PasswordPolicy passwordPolicy;
    private final LockPolicy lockPolicy;

    public LoginProcessSharedServiceImpl(AuthUserRepository authUserRepository,
                                         AuthLoginHistoryRepository loginHistoryRepository,
                                         AuthPasswordHistoryRepository passwordHistoryRepository,
                                         AuthAccountLockHistoryRepository lockHistoryRepository,
                                         PasswordPolicy passwordPolicy,
                                         LockPolicy lockPolicy) {
        this.authUserRepository = authUserRepository;
        this.loginHistoryRepository = loginHistoryRepository;
        this.passwordHistoryRepository = passwordHistoryRepository;
        this.lockHistoryRepository = lockHistoryRepository;
        this.passwordPolicy = passwordPolicy;
        this.lockPolicy = lockPolicy;
    }

    @Override
    public LoginSuccessResult onLoginSuccess(String loginIdValue) {
        LoginId loginId = new LoginId(loginIdValue);
        AuthUser user = authUserRepository.findByLoginId(loginId)
                .orElseThrow(() -> new AuthDomainException("ログイン成功後にユーザ情報が取得できません。"));

        AuthUserId userId = user.id();
        if (userId == null) {
            throw new AuthDomainException("ユーザID未採番のためログイン履歴を記録できません。");
        }

        LocalDateTime now = LocalDateTime.now();

        // ログイン成功履歴を登録
        LoginHistory successHistory = LoginHistory.success(
                userId,
                now,
                loginId
        );
        loginHistoryRepository.save(successHistory);

        // パスワード変更が必要かどうか判定
        boolean mustChange = isPasswordChangeRequired(userId, now);

        return new LoginSuccessResult(mustChange);
    }

    @Override
    public LoginFailureType onLoginFailure(String loginIdValue) {

        if (loginIdValue == null || loginIdValue.isBlank()) {
            // ログインID無し → ユーザ特定せず BAD_CREDENTIALS
            return LoginFailureType.BAD_CREDENTIALS;
        }

        LoginId loginId = new LoginId(loginIdValue);
        Optional<AuthUser> optUser = authUserRepository.findByLoginId(loginId);

        if (optUser.isEmpty()) {
            // ユーザが存在しない場合は履歴を残さない（情報漏洩防止）
            return LoginFailureType.BAD_CREDENTIALS;
        }

        AuthUser user = optUser.get();
        AuthUserId userId = user.id();
        if (userId == null) {
            return LoginFailureType.BAD_CREDENTIALS;
        }

        LocalDateTime now = LocalDateTime.now();

        // ◆ ロックイベント一覧から現在のロック状態を判定
        AccountLockEvents lockEvents = lockHistoryRepository.findByUserId(userId);

        if (lockEvents.isLocked()) {
            // ロック中のログインは LOCKED として履歴のみ（失敗カウントには含めない）
            LoginHistory lockedHistory = LoginHistory.locked(
                    userId,
                    now,
                    loginId
            );
            loginHistoryRepository.save(lockedHistory);
            return LoginFailureType.LOCKED;
        }

        // まだロックされていない場合 → FAIL として履歴を追加しつつ、ポリシーでロック判定

        // 直近の履歴を「そこそこ十分な件数」取得
        int limit = 20; // 6回しきい値ならこの程度で充分
        LoginHistories recentHistories =
                loginHistoryRepository.findRecentByUserId(userId, limit);

        // 今回の FAIL を先頭に付ける
        LoginHistory failHistory = LoginHistory.fail(
                userId,
                now,
                loginId
        );

        LoginHistories loginHistories = recentHistories.add(failHistory);

        // 最後の UNLOCK 時刻（なければ null）
        LocalDateTime lastUnlockAt = lockEvents.lastUnlockAt().orElse(null);

        // LockPolicy（ポリシーパターン）でロックアウト判定
        boolean shouldLockout = lockPolicy.isLockout(loginHistories, lastUnlockAt);

        // まず FAIL 履歴を保存
        loginHistoryRepository.save(failHistory);

        if (shouldLockout) {
            // ロックイベント追加
            AccountLockEvent lockEvent = AccountLockEvent.lock(
                    userId,
                    now,
                    "LOGIN_FAIL_THRESHOLD",
                    loginId
            );
            lockHistoryRepository.save(lockEvent);
            return LoginFailureType.LOCKED;
        } else {
            return LoginFailureType.BAD_CREDENTIALS;
        }
    }



    /**
     * パスワード変更強制が必要かどうか判定する。
     * - 履歴なし → 強制（安全側）
     * - 履歴が INITIAL_REGISTER / ADMIN_RESET → 強制
     * - 履歴が USER_CHANGE で、有効期限切れ → 強制
     */
    private boolean isPasswordChangeRequired(AuthUserId userId, LocalDateTime now) {
        Optional<PasswordHistory> optLast = passwordHistoryRepository.findLastByUserId(userId);

        if (optLast.isEmpty()) {
            return true;
        }

        PasswordHistory last = optLast.get();
        if (last.isPasswordChangeRequired()) {
            return true;
        }

        return passwordPolicy.isExpired(last.changedAt(), now);
    }
}
