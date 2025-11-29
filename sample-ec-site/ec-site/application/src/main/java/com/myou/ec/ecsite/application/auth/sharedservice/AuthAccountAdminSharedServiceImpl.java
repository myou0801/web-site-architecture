package com.myou.ec.ecsite.application.auth.sharedservice;

import com.myou.ec.ecsite.domain.auth.exception.AuthDomainException;
import com.myou.ec.ecsite.domain.auth.model.AccountLockEvent;
import com.myou.ec.ecsite.domain.auth.model.AccountLockEvents;
import com.myou.ec.ecsite.domain.auth.model.AuthAccount;
import com.myou.ec.ecsite.domain.auth.model.PasswordHistory;
import com.myou.ec.ecsite.domain.auth.model.value.AuthAccountId;
import com.myou.ec.ecsite.domain.auth.model.value.EncodedPassword;
import com.myou.ec.ecsite.domain.auth.model.value.RoleCode;
import com.myou.ec.ecsite.domain.auth.model.value.UserId;
import com.myou.ec.ecsite.domain.auth.repository.AuthAccountLockHistoryRepository;
import com.myou.ec.ecsite.domain.auth.repository.AuthAccountRepository;
import com.myou.ec.ecsite.domain.auth.repository.AuthPasswordHistoryRepository;
import com.myou.ec.ecsite.domain.auth.repository.AuthRoleRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class AuthAccountAdminSharedServiceImpl implements AuthAccountAdminSharedService {

    private final AuthAccountRepository authAccountRepository;
    private final AuthRoleRepository authRoleRepository;
    private final AuthPasswordHistoryRepository passwordHistoryRepository;
    private final AuthAccountLockHistoryRepository lockHistoryRepository;
    private final PasswordEncoder passwordEncoder;
    private final String initialPassword;

    public AuthAccountAdminSharedServiceImpl(AuthAccountRepository authAccountRepository,
                                             AuthRoleRepository authRoleRepository,
                                             AuthPasswordHistoryRepository passwordHistoryRepository,
                                             AuthAccountLockHistoryRepository lockHistoryRepository,
                                             PasswordEncoder passwordEncoder,
                                             @Value("${auth.initial-password:password123}") String initialPassword) {
        this.authAccountRepository = authAccountRepository;
        this.authRoleRepository = authRoleRepository;
        this.passwordHistoryRepository = passwordHistoryRepository;
        this.lockHistoryRepository = lockHistoryRepository;
        this.passwordEncoder = passwordEncoder;
        this.initialPassword = initialPassword;
    }

    @Override
    public AuthAccountId registerAccount(UserId userId,
                                    List<RoleCode> roleCodes,
                                    UserId operator) {

        // パスワードハッシュ化
        EncodedPassword encodedPassword = new EncodedPassword(passwordEncoder.encode(initialPassword));

        LocalDateTime now = LocalDateTime.now();
        // AuthAccount 作成 & 保存
        AuthAccount user = AuthAccount.newAccount(userId, encodedPassword, roleCodes, now, operator);
        authAccountRepository.save(user);

        // ID 採番後のアカウントを再取得（ID 必要なため）
        AuthAccount savedUser = authAccountRepository.findByUserId(userId)
                .orElseThrow(() -> new AuthDomainException("アカウント登録後の再取得に失敗しました。"));

        AuthAccountId accountId = savedUser.id();
        if (accountId == null) {
            throw new AuthDomainException("採番されたアカウントIDが取得できません。");
        }

        // ユーザロール設定
        authRoleRepository.saveAccountRoles(accountId, roleCodes);

        // パスワード履歴登録（初回登録）
        PasswordHistory history = PasswordHistory.initialRegister(
                accountId,
                encodedPassword,
                now,
                operator
        );
        passwordHistoryRepository.save(history);

        return accountId;
    }

    @Override
    public void resetPasswordToInitial(AuthAccountId targetAccountId, UserId operator) {
        AuthAccount user = authAccountRepository.findById(targetAccountId)
                .orElseThrow(() -> new AuthDomainException("対象アカウントが存在しません。"));


        // パスワードハッシュ化
        EncodedPassword encodedPassword = new EncodedPassword(passwordEncoder.encode(initialPassword));

        // パスワード更新
        user.changePassword(encodedPassword);
        authAccountRepository.save(user);

        LocalDateTime now = LocalDateTime.now();

        // パスワード履歴（ADMIN_RESET）
        PasswordHistory history = PasswordHistory.adminReset(
                targetAccountId,
                encodedPassword,
                now,
                operator
        );
        passwordHistoryRepository.save(history);

        // ロック解除イベント（パスワード初期化時はロック解除も行う）
        AccountLockEvent unlockEvent = AccountLockEvent.unlock(
                targetAccountId,
                now,
                "ADMIN_RESET_AND_UNLOCK",
                operator
        );
        lockHistoryRepository.save(unlockEvent);
    }

    @Override
    public void unlockAccount(AuthAccountId targetAccountId, UserId operator) {
        AuthAccount user = authAccountRepository.findById(targetAccountId)
                .orElseThrow(() -> new AuthDomainException("対象アカウントが存在しません。"));


        AccountLockEvents events = lockHistoryRepository.findByAccountId(user.id(),20);
        if (!events.isLocked()) {
            // 既に未ロックなら何もしない（イベントを増やさない方針）
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        AccountLockEvent unlockEvent = AccountLockEvent.unlock(
                user.id(),
                now,
                "ADMIN_UNLOCK",
                operator
        );
        lockHistoryRepository.save(unlockEvent);
    }

    @Override
    public void disableAccount(AuthAccountId targetAccountId, UserId operator) {
        AuthAccount user = authAccountRepository.findById(targetAccountId)
                .orElseThrow(() -> new AuthDomainException("対象アカウントが存在しません。"));

        user.disable();
        authAccountRepository.save(user);
    }

    @Override
    public void enableAccount(AuthAccountId targetAccountId, UserId operator) {
        AuthAccount user = authAccountRepository.findById(targetAccountId)
                .orElseThrow(() -> new AuthDomainException("対象アカウントが存在しません。"));

        user.enable();
        authAccountRepository.save(user);
    }
}
