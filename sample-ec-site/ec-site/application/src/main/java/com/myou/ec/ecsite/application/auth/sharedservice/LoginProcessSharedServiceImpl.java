package com.myou.ec.ecsite.application.auth.sharedservice;


import com.myou.ec.ecsite.domain.auth.exception.AuthDomainException;
import com.myou.ec.ecsite.domain.auth.model.*;
import com.myou.ec.ecsite.domain.auth.model.value.AuthAccountId;
import com.myou.ec.ecsite.domain.auth.model.value.Operator;
import com.myou.ec.ecsite.domain.auth.model.value.UserId;
import com.myou.ec.ecsite.domain.auth.policy.LockPolicy;
import com.myou.ec.ecsite.domain.auth.repository.AuthAccountExpiryHistoryRepository;
import com.myou.ec.ecsite.domain.auth.repository.AuthAccountLockHistoryRepository;
import com.myou.ec.ecsite.domain.auth.repository.AuthAccountRepository;
import com.myou.ec.ecsite.domain.auth.repository.AuthLoginHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Transactional
public class LoginProcessSharedServiceImpl implements LoginProcessSharedService {

    private final AuthAccountRepository authAccountRepository;
    private final AuthLoginHistoryRepository loginHistoryRepository;
    private final AuthAccountLockHistoryRepository lockHistoryRepository;
    private final AuthAccountExpiryHistoryRepository accountExpiryHistoryRepository;
    private final LockPolicy lockPolicy;
    private final Clock clock;

    public LoginProcessSharedServiceImpl(AuthAccountRepository authAccountRepository,
                                         AuthLoginHistoryRepository loginHistoryRepository,
                                         AuthAccountLockHistoryRepository lockHistoryRepository, AuthAccountExpiryHistoryRepository accountExpiryHistoryRepository,
                                         LockPolicy lockPolicy, Clock clock) {
        this.authAccountRepository = authAccountRepository;
        this.loginHistoryRepository = loginHistoryRepository;
        this.lockHistoryRepository = lockHistoryRepository;
        this.accountExpiryHistoryRepository = accountExpiryHistoryRepository;
        this.lockPolicy = lockPolicy;
        this.clock = clock;
    }

    @Override
    public void onLoginSuccess(UserId userId) {
        AuthAccount user = authAccountRepository.findByUserId(userId)
                .orElseThrow(() -> new AuthDomainException("ログイン成功後にアカウント情報が取得できません。"));

        AuthAccountId accountId = user.id();
        if (accountId == null) {
            throw new AuthDomainException("アカウントID未採番のためログイン履歴を記録できません。");
        }

        LocalDateTime now = LocalDateTime.now(clock);

        // ログイン成功履歴を登録
        LoginHistory successHistory = LoginHistory.success(
                accountId,
                now
        );
        loginHistoryRepository.save(successHistory, Operator.ofUserId(userId));
    }

    @Override
    public void onLoginFailure(UserId userId) {

        if (userId == null) {
            // ユーザーIDが入力されていない場合は、アカウントを特定できないため履歴を残さず終了。
            return;
        }

        Optional<AuthAccount> optUser = authAccountRepository.findByUserId(userId);

        if (optUser.isEmpty()) {
            // アカウントが存在しない場合は、どのアカウントの失敗かを特定できないため履歴を残さず終了（総当たり攻撃への情報漏洩対策）。
            return;
        }

        AuthAccount user = optUser.get();
        AuthAccountId accountId = user.id();
        if (accountId == null) {
            // アカウントは存在するがIDが未採番。基本ありえないが念のため。
            return;
        }

        LocalDateTime now = LocalDateTime.now(clock);

        AccountExpiryEvents expiryEvents = accountExpiryHistoryRepository.findByAccountId(accountId);

        Operator operator = Operator.of(userId.value());

        // アカウント有効期限切れ
        if (expiryEvents.isExpired()) {
            loginHistoryRepository.save(LoginHistory.expired(accountId, now), operator);
            return;
        }

        if (!user.canLogin()) {
            // ★ DISABLED として履歴を残し、ロック判定はしない
            loginHistoryRepository.save(LoginHistory.disabled(accountId, now), operator);
            return;
        }

        // ◆ ロックイベント一覧から現在のロック状態を判定
        AccountLockEvents lockEvents = lockHistoryRepository.findByAccountId(accountId, 20);

        if (lockEvents.isLocked()) {
            // すでにロック中のアカウントによるログイン試行は 'LOCKED' として履歴のみ記録（連続失敗カウントには影響しない）。
            LoginHistory lockedHistory = LoginHistory.locked(
                    accountId,
                    now
            );
            loginHistoryRepository.save(lockedHistory, operator);
            return;
        }

        // まだロックされていない場合 → 'FAIL' として履歴を追加し、ロックアウトポリシーに基づきロック要否を判定。

        // 直近の履歴を「そこそこ十分な件数」取得
        int limit = 20; // ポリシーのしきい値が6回ならこの程度で充分
        LoginHistories recentHistories =
                loginHistoryRepository.findRecentByAccountId(accountId, limit);

        // 今回の 'FAIL' 履歴を生成
        LoginHistory failHistory = LoginHistory.fail(
                accountId,
                now
            );

        // 今回の失敗を加えて、履歴コレクションを再構成
        LoginHistories loginHistories = recentHistories.add(failHistory);

        // 最後のロック解除日時を取得（なければ null）
        LocalDateTime lastUnlockAt = lockEvents.lastUnlockAt().orElse(null);

        // LockPolicy（ドメイン知識）でロックアウトすべきか判定
        boolean shouldLockout = lockPolicy.isLockout(loginHistories, lastUnlockAt);

        // まず今回の 'FAIL' 履歴を保存
        loginHistoryRepository.save(failHistory, operator);

        if (shouldLockout) {
            // ポリシー違反なら、ロックイベントを追加
            AccountLockEvent lockEvent = AccountLockEvent.lock(
                    accountId,
                    now,
                    "LOGIN_FAIL_THRESHOLD", // ロック理由
                    Operator.ofUserId(userId)
            );
            lockHistoryRepository.save(lockEvent, operator);
        }
    }


}
