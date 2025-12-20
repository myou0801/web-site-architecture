package com.myou.ec.ecsite.application.auth.sharedservice;

import com.myou.ec.ecsite.application.auth.provider.CurrentUserProvider;
import com.myou.ec.ecsite.domain.auth.exception.AuthDomainException;
import com.myou.ec.ecsite.domain.auth.model.*;
import com.myou.ec.ecsite.domain.auth.model.value.*;
import com.myou.ec.ecsite.domain.auth.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Set;

@Service
@Transactional
public class AuthAccountAdminSharedServiceImpl implements AuthAccountAdminSharedService {

    private final AuthAccountRepository authAccountRepository;
    private final AuthAccountRoleRepository authAccountRoleRepository;
    private final AuthPasswordHistoryRepository passwordHistoryRepository;
    private final AuthAccountLockHistoryRepository lockHistoryRepository;
    private final AuthAccountStatusHistoryRepository statusHistoryRepository;
    private final AccountExpirySharedService accountExpirySharedService;
    private final CurrentUserProvider currentUserProvider;
    private final PasswordEncoder passwordEncoder;
    private final String initialPassword;
    private final Clock clock;

    public AuthAccountAdminSharedServiceImpl(AuthAccountRepository authAccountRepository, AuthAccountRoleRepository authAccountRoleRepository,

                                             AuthPasswordHistoryRepository passwordHistoryRepository,
                                             AuthAccountLockHistoryRepository lockHistoryRepository,
                                             AuthAccountStatusHistoryRepository statusHistoryRepository,
                                             AccountExpirySharedService accountExpirySharedService, CurrentUserProvider currentUserProvider,
                                             PasswordEncoder passwordEncoder,
                                             @Value("${auth.initial-password:password123}") String initialPassword, Clock clock) {
        this.authAccountRepository = authAccountRepository;
        this.authAccountRoleRepository = authAccountRoleRepository;
        this.passwordHistoryRepository = passwordHistoryRepository;
        this.lockHistoryRepository = lockHistoryRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.accountExpirySharedService = accountExpirySharedService;
        this.currentUserProvider = currentUserProvider;
        this.passwordEncoder = passwordEncoder;
        this.initialPassword = initialPassword;
        this.clock = clock;
    }

    @Override
    public AuthAccountId registerAccount(UserId newUserId,
                                         Set<RoleCode> roleCodes) {

        Operator operator = currentUserProvider.currentOrSystem();

        // パスワードハッシュ化
        PasswordHash passwordHash = new PasswordHash(passwordEncoder.encode(initialPassword));

        LocalDateTime now = LocalDateTime.now(clock);
        // AuthAccount 作成 & 保存
        AuthAccount user = AuthAccount.newAccount(newUserId, passwordHash);
        authAccountRepository.save(user, operator);

        // ID 採番後のアカウントを再取得（ID 必要なため）
        AuthAccount savedUser = authAccountRepository.findByUserId(newUserId)
                .orElseThrow(() -> new AuthDomainException("アカウント登録後の再取得に失敗しました。"));

        AuthAccountId accountId = savedUser.id();
        if (accountId == null) {
            throw new AuthDomainException("採番されたアカウントIDが取得できません。");
        }

        // ユーザロール設定
        roleCodes.forEach(roleCode -> authAccountRoleRepository.addRole(accountId, roleCode, operator)); // Pass Operator directly
        
        // パスワード履歴登録（初回登録）
        PasswordHistory history = PasswordHistory.initialRegister(
                accountId,
                passwordHash,
                now,
                operator
        );
        passwordHistoryRepository.save(history, operator);

        // ステータス履歴登録（初回登録）
        AuthAccountStatusHistory statusHistory = AuthAccountStatusHistory.forNewAccount(
                accountId,
                now,
                operator,
                "REGISTER_ACCOUNT"
        );
        statusHistoryRepository.save(statusHistory, operator);

        return accountId;
    }

    @Override
    public void resetPassword(AuthAccountId targetAccountId) {
        AuthAccount user = authAccountRepository.findById(targetAccountId)
                .orElseThrow(() -> new AuthDomainException("対象アカウントが存在しません。"));

        Operator operator = currentUserProvider.currentOrSystem();

        // パスワードハッシュ化
        PasswordHash passwordHash = new PasswordHash(passwordEncoder.encode(initialPassword));

        LocalDateTime now = LocalDateTime.now(clock);

        // パスワード更新
        authAccountRepository.save(user.changePassword(passwordHash), operator);

        // パスワード履歴（ADMIN_RESET）
        PasswordHistory history = PasswordHistory.adminReset(
                targetAccountId,
                passwordHash,
                now,
                operator
        );
        passwordHistoryRepository.save(history, operator);

        // ロック解除イベント（パスワード初期化時はロック解除も行う）
        AccountLockEvent unlockEvent = AccountLockEvent.unlock(
                targetAccountId,
                now,
                "ADMIN_RESET_AND_UNLOCK",
                operator
        );
        lockHistoryRepository.save(unlockEvent, operator);
    }

    @Override
    public void unlockAccount(AuthAccountId targetAccountId) {
        AuthAccount user = authAccountRepository.findById(targetAccountId)
                .orElseThrow(() -> new AuthDomainException("対象アカウントが存在しません。"));


        AccountLockEvents events = lockHistoryRepository.findByAccountId(user.id(), 20);
        if (!events.isLocked()) {
            // 既に未ロックなら何もしない（イベントを増やさない方針）
            return;
        }

        Operator operator = currentUserProvider.currentOrSystem();

        LocalDateTime now = LocalDateTime.now(clock);

        AccountLockEvent unlockEvent = AccountLockEvent.unlock(
                user.id(),
                now,
                "ADMIN_UNLOCK",
                operator
        );
        lockHistoryRepository.save(unlockEvent, operator);
    }

    @Override
    public void disableAccount(AuthAccountId targetAccountId) {
        AuthAccount user = authAccountRepository.findById(targetAccountId)
                .orElseThrow(() -> new AuthDomainException("対象アカウントが存在しません。"));

        // すでに無効化されているユーザは処理不要
        if(!user.accountStatus().canTransitionTo(AccountStatus.DISABLED)){
            return;
        }

        Operator operator = currentUserProvider.currentOrSystem();

        LocalDateTime now = LocalDateTime.now(clock);
        AuthAccount disabledUser = user.disable();
        authAccountRepository.save(disabledUser, operator);

        AuthAccountStatusHistory statusHistory = AuthAccountStatusHistory.forDisabling(
                targetAccountId,
                user.accountStatus(),
                now,
                operator,
                "DISABLE_ACCOUNT"
        );
        statusHistoryRepository.save(statusHistory, operator);
    }

    @Override
    public void enableAccount(AuthAccountId targetAccountId) {
        AuthAccount user = authAccountRepository.findById(targetAccountId)
                .orElseThrow(() -> new AuthDomainException("対象アカウントが存在しません。"));

        // 有効期限切れのユーザも有効化する
        accountExpirySharedService.unexpireIfExpired(targetAccountId);

        // すでに有効化されているユーザは処理不要
        if(!user.accountStatus().canTransitionTo(AccountStatus.ACTIVE)){
            return;
        }

        Operator operator = currentUserProvider.currentOrSystem();
        LocalDateTime now = LocalDateTime.now(clock);

        authAccountRepository.save(user.activate(), operator);

        AuthAccountStatusHistory statusHistory = AuthAccountStatusHistory.forActivating(
                targetAccountId,
                user.accountStatus(),
                now,
                operator,
                "ENABLE_ACCOUNT"
        );
        statusHistoryRepository.save(statusHistory, operator);
    }

    @Override
    public void addRole(AuthAccountId targetAccountId, RoleCode role) {

    }

    @Override
    public void removeRole(AuthAccountId targetAccountId, RoleCode role) {

    }

    @Override
    public void deleteAccount(AuthAccountId targetAccountId) {
        AuthAccount user = authAccountRepository.findById(targetAccountId)
                .orElseThrow(() -> new AuthDomainException("対象アカウントが存在しません。"));

        // すでに削除されているユーザは処理不要
        if(!user.accountStatus().canTransitionTo(AccountStatus.DELETED)){
            return;
        }

        Operator operator = currentUserProvider.currentOrSystem();

        LocalDateTime now = LocalDateTime.now(clock);
        AuthAccount deletedUser = user.markAsDeleted();
        authAccountRepository.save(deletedUser, operator);

        AuthAccountStatusHistory statusHistory = AuthAccountStatusHistory.forDeleting(
                targetAccountId,
                user.accountStatus(),
                now,
                operator,
                "DELETE_ACCOUNT"
        );
        statusHistoryRepository.save(statusHistory, operator);
    }
}

