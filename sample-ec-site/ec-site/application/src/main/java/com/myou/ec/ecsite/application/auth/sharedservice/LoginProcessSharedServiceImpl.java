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
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Transactional
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
    public void onLoginSuccess(LoginId loginId) {
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
    }

    @Override
    public void onLoginFailure(LoginId loginId) {

        if (loginId == null) {
            // ログインIDが入力されていない場合は、ユーザを特定できないため履歴を残さず終了。
            return;
        }

        Optional<AuthUser> optUser = authUserRepository.findByLoginId(loginId);

        if (optUser.isEmpty()) {
            // ユーザが存在しない場合は、どのユーザの失敗かを特定できないため履歴を残さず終了（総当たり攻撃への情報漏洩対策）。
            return;
        }

        AuthUser user = optUser.get();
        AuthUserId userId = user.id();
        if (userId == null) {
            // ユーザは存在するがIDが未採番。基本ありえないが念のため。
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        // ◆ ロックイベント一覧から現在のロック状態を判定
        AccountLockEvents lockEvents = lockHistoryRepository.findByUserId(userId);

        if (lockEvents.isLocked()) {
            // すでにロック中のユーザによるログイン試行は 'LOCKED' として履歴のみ記録（連続失敗カウントには影響しない）。
            LoginHistory lockedHistory = LoginHistory.locked(
                    userId,
                    now,
                    loginId
            );
            loginHistoryRepository.save(lockedHistory);
            return;
        }

        // まだロックされていない場合 → 'FAIL' として履歴を追加し、ロックアウトポリシーに基づきロック要否を判定。

        // 直近の履歴を「そこそこ十分な件数」取得
        int limit = 20; // ポリシーのしきい値が6回ならこの程度で充分
        LoginHistories recentHistories =
                loginHistoryRepository.findRecentByUserId(userId, limit);

        // 今回の 'FAIL' 履歴を生成
        LoginHistory failHistory = LoginHistory.fail(
                userId,
                now,
                loginId
        );

        // 今回の失敗を加えて、履歴コレクションを再構成
        LoginHistories loginHistories = recentHistories.add(failHistory);

        // 最後のロック解除日時を取得（なければ null）
        LocalDateTime lastUnlockAt = lockEvents.lastUnlockAt().orElse(null);

        // LockPolicy（ドメイン知識）でロックアウトすべきか判定
        boolean shouldLockout = lockPolicy.isLockout(loginHistories, lastUnlockAt);

        // まず今回の 'FAIL' 履歴を保存
        loginHistoryRepository.save(failHistory);

        if (shouldLockout) {
            // ポリシー違反なら、ロックイベントを追加
            AccountLockEvent lockEvent = AccountLockEvent.lock(
                    userId,
                    now,
                    "LOGIN_FAIL_THRESHOLD", // ロック理由
                    loginId
            );
            lockHistoryRepository.save(lockEvent);
        }
    }


    @Override
    public boolean isPasswordChangeRequired(LoginId loginId) {

        Optional<PasswordHistory> optLast = authUserRepository.findByLoginId(loginId)
                .flatMap(user -> passwordHistoryRepository.findLastByUserId(user.id()));

        if (optLast.isEmpty()) {
            return true;
        }

        PasswordHistory last = optLast.get();
        if (last.isPasswordChangeRequired()) {
            return true;
        }

        LocalDateTime now = LocalDateTime.now();

        return passwordPolicy.isExpired(last.changedAt(), now);
    }
}
