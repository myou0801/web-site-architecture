package com.myou.ec.ecsite.application.auth.sharedservice;

import com.myou.ec.ecsite.domain.auth.exception.AuthDomainException;
import com.myou.ec.ecsite.domain.auth.model.AccountLockEvent;
import com.myou.ec.ecsite.domain.auth.model.AccountLockEvents;
import com.myou.ec.ecsite.domain.auth.model.AuthUser;
import com.myou.ec.ecsite.domain.auth.model.PasswordHistory;
import com.myou.ec.ecsite.domain.auth.model.policy.PasswordPolicy;
import com.myou.ec.ecsite.domain.auth.model.value.AuthUserId;
import com.myou.ec.ecsite.domain.auth.model.value.EncodedPassword;
import com.myou.ec.ecsite.domain.auth.model.value.LoginId;
import com.myou.ec.ecsite.domain.auth.model.value.RoleCode;
import com.myou.ec.ecsite.domain.auth.repository.AuthAccountLockHistoryRepository;
import com.myou.ec.ecsite.domain.auth.repository.AuthPasswordHistoryRepository;
import com.myou.ec.ecsite.domain.auth.repository.AuthRoleRepository;
import com.myou.ec.ecsite.domain.auth.repository.AuthUserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class AuthAccountAdminSharedServiceImpl implements AuthAccountAdminSharedService {

    private final AuthUserRepository authUserRepository;
    private final AuthRoleRepository authRoleRepository;
    private final AuthPasswordHistoryRepository passwordHistoryRepository;
    private final AuthAccountLockHistoryRepository lockHistoryRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicy passwordPolicy;
    private final String initialPassword;

    public AuthAccountAdminSharedServiceImpl(AuthUserRepository authUserRepository,
                                             AuthRoleRepository authRoleRepository,
                                             AuthPasswordHistoryRepository passwordHistoryRepository,
                                             AuthAccountLockHistoryRepository lockHistoryRepository,
                                             PasswordEncoder passwordEncoder,
                                             PasswordPolicy passwordPolicy,
                                             @Value("${auth.initial-password:password123}") String initialPassword) {
        this.authUserRepository = authUserRepository;
        this.authRoleRepository = authRoleRepository;
        this.passwordHistoryRepository = passwordHistoryRepository;
        this.lockHistoryRepository = lockHistoryRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicy = passwordPolicy;
        this.initialPassword = initialPassword;
    }

    @Override
    public AuthUserId registerAccount(LoginId loginId,
                                    List<RoleCode> roleCodes,
                                    LoginId operator) {

        // パスワードハッシュ化
        EncodedPassword encodedPassword = new EncodedPassword(passwordEncoder.encode(initialPassword));

        LocalDateTime now = LocalDateTime.now();
        // AuthUser 作成 & 保存
        AuthUser user = AuthUser.newUser(loginId, encodedPassword, roleCodes, now, operator);
        authUserRepository.save(user);

        // ID 採番後のユーザを再取得（ID 必要なため）
        AuthUser savedUser = authUserRepository.findByLoginId(loginId)
                .orElseThrow(() -> new AuthDomainException("アカウント登録後の再取得に失敗しました。"));

        AuthUserId userId = savedUser.id();
        if (userId == null) {
            throw new AuthDomainException("採番されたユーザIDが取得できません。");
        }

        // ユーザロール設定
        authRoleRepository.saveUserRoles(userId, roleCodes);

        // パスワード履歴登録（初回登録）
        PasswordHistory history = PasswordHistory.initialRegister(
                userId,
                encodedPassword,
                now,
                operator
        );
        passwordHistoryRepository.save(history);

        return userId;
    }

    @Override
    public void resetPasswordToInitial(AuthUserId targetUserId, LoginId operator) {
        AuthUser user = authUserRepository.findById(targetUserId)
                .orElseThrow(() -> new AuthDomainException("対象アカウントが存在しません。"));


        // パスワードハッシュ化
        EncodedPassword encodedPassword = new EncodedPassword(passwordEncoder.encode(initialPassword));

        // パスワード更新
        user.changePassword(encodedPassword);
        authUserRepository.save(user);

        LocalDateTime now = LocalDateTime.now();

        // パスワード履歴（ADMIN_RESET）
        PasswordHistory history = PasswordHistory.adminReset(
                targetUserId,
                encodedPassword,
                now,
                operator
        );
        passwordHistoryRepository.save(history);

        // ロック解除イベント（パスワード初期化時はロック解除も行う）
        AccountLockEvent unlockEvent = AccountLockEvent.unlock(
                targetUserId,
                now,
                "ADMIN_RESET_AND_UNLOCK",
                operator
        );
        lockHistoryRepository.save(unlockEvent);
    }

    @Override
    public void unlockAccount(AuthUserId targetUserId, LoginId operator) {
        AuthUser user = authUserRepository.findById(targetUserId)
                .orElseThrow(() -> new AuthDomainException("対象アカウントが存在しません。"));


        AccountLockEvents events = lockHistoryRepository.findByUserId(user.id(),20);
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
    public void disableAccount(AuthUserId targetUserId, LoginId operator) {
        AuthUser user = authUserRepository.findById(targetUserId)
                .orElseThrow(() -> new AuthDomainException("対象アカウントが存在しません。"));

        user.disable();
        authUserRepository.save(user);
    }

    @Override
    public void enableAccount(AuthUserId targetUserId, LoginId operator) {
        AuthUser user = authUserRepository.findById(targetUserId)
                .orElseThrow(() -> new AuthDomainException("対象アカウントが存在しません。"));

        user.enable();
        authUserRepository.save(user);
    }
}
