package com.myou.ec.ecsite.application.auth.sharedservice;


import com.myou.ec.ecsite.domain.auth.exception.AuthDomainException;
import com.myou.ec.ecsite.domain.auth.model.*;
import com.myou.ec.ecsite.domain.auth.model.value.AuthAccountId;
import com.myou.ec.ecsite.domain.auth.model.value.Operator;
import com.myou.ec.ecsite.domain.auth.model.value.UserId;
import com.myou.ec.ecsite.domain.auth.policy.LockPolicy;
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
    private final AccountExpirySharedService accountExpirySharedService;
    private final LockPolicy lockPolicy;
    private final Clock clock;

    public LoginProcessSharedServiceImpl(AuthAccountRepository authAccountRepository,
                                         AuthLoginHistoryRepository loginHistoryRepository,
                                         AuthAccountLockHistoryRepository lockHistoryRepository,
                                         AccountExpirySharedService accountExpirySharedService,
                                         LockPolicy lockPolicy, Clock clock) {
        this.authAccountRepository = authAccountRepository;
        this.loginHistoryRepository = loginHistoryRepository;
        this.lockHistoryRepository = lockHistoryRepository;
        this.accountExpirySharedService = accountExpirySharedService;
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
        AuthAccount user = findAccount(userId);
        if (user == null) {
            return;
        }

        AuthAccountId accountId = user.id();
        if (accountId == null) {
            return;
        }

        LocalDateTime now = LocalDateTime.now(clock);
        Operator operator = Operator.of(userId.value());

        // 有効期限切れの判定と更新
        accountExpirySharedService.expireIfNeeded(accountId);
        if (accountExpirySharedService.isExpired(accountId)) {
            saveLoginHistory(LoginHistory.expired(accountId, now), operator);
            return;
        }

        if (!user.canLogin()) {
            saveLoginHistory(LoginHistory.disabled(accountId, now), operator);
            return;
        }

        AccountLockEvents lockEvents = lockHistoryRepository.findByAccountId(accountId, 20);
        if (lockEvents.isLocked()) {
            saveLoginHistory(LoginHistory.locked(accountId, now), operator);
            return;
        }

        processFailureAndLockout(accountId, userId, now, lockEvents, operator);
    }

    private AuthAccount findAccount(UserId userId) {
        return Optional.ofNullable(userId)
                .flatMap(authAccountRepository::findByUserId)
                .orElse(null);
    }

    private void saveLoginHistory(LoginHistory history, Operator operator) {
        loginHistoryRepository.save(history, operator);
    }

    private void processFailureAndLockout(AuthAccountId accountId, UserId userId, LocalDateTime now, AccountLockEvents lockEvents, Operator operator) {
        // 直近の履歴を取得
        LoginHistories recentHistories = loginHistoryRepository.findRecentByAccountId(accountId, 20);

        // 今回の失敗履歴
        LoginHistory failHistory = LoginHistory.fail(accountId, now);

        // 履歴を追加して判定用コレクションを作成
        LoginHistories loginHistories = recentHistories.add(failHistory);

        // ロックアウト判定
        LocalDateTime lastUnlockAt = lockEvents.lastUnlockAt().orElse(null);
        boolean shouldLockout = lockPolicy.isLockout(loginHistories, lastUnlockAt);

        // 失敗履歴保存
        saveLoginHistory(failHistory, operator);

        // ロックアウト処理
        if (shouldLockout) {
            AccountLockEvent lockEvent = AccountLockEvent.lock(
                    accountId,
                    now,
                    "LOGIN_FAIL_THRESHOLD",
                    Operator.ofUserId(userId)
            );
            lockHistoryRepository.save(lockEvent, operator);
        }
    }
}
