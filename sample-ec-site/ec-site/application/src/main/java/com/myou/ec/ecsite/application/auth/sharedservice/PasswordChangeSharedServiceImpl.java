package com.myou.ec.ecsite.application.auth.sharedservice;

import com.myou.ec.ecsite.domain.auth.exception.AuthDomainException;
import com.myou.ec.ecsite.domain.auth.exception.PasswordReuseNotAllowedException;
import com.myou.ec.ecsite.domain.auth.model.AuthUser;
import com.myou.ec.ecsite.domain.auth.model.PasswordHistory;
import com.myou.ec.ecsite.domain.auth.model.policy.ExpiredPasswordPolicy;
import com.myou.ec.ecsite.domain.auth.model.policy.PasswordPolicy;
import com.myou.ec.ecsite.domain.auth.model.value.AuthUserId;
import com.myou.ec.ecsite.domain.auth.model.value.EncodedPassword;
import com.myou.ec.ecsite.domain.auth.model.value.LoginId;
import com.myou.ec.ecsite.domain.auth.repository.AuthPasswordHistoryRepository;
import com.myou.ec.ecsite.domain.auth.repository.AuthUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class PasswordChangeSharedServiceImpl implements PasswordChangeSharedService {

    private final AuthUserContextSharedService userContextSharedService;
    private final AuthUserRepository authUserRepository;
    private final AuthPasswordHistoryRepository passwordHistoryRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicy passwordPolicy;

    public PasswordChangeSharedServiceImpl(AuthUserContextSharedService userContextSharedService,
                                           AuthUserRepository authUserRepository,
                                           AuthPasswordHistoryRepository passwordHistoryRepository,
                                           PasswordEncoder passwordEncoder,
                                           PasswordPolicy passwordPolicy) {
        this.userContextSharedService = userContextSharedService;
        this.authUserRepository = authUserRepository;
        this.passwordHistoryRepository = passwordHistoryRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicy = passwordPolicy;
    }



    @Override
    @Transactional(readOnly = true)
    public PasswordChangeRequirementType requirementOf(LoginId loginId) {

        Optional<PasswordHistory> optLast = authUserRepository.findByLoginId(loginId)
                .flatMap(user -> passwordHistoryRepository.findLastByUserId(user.id()));

        if (optLast.isEmpty()) {
            return PasswordChangeRequirementType.INITIAL_REGISTER;
        }

        PasswordHistory last = optLast.get();
        return switch (last.changeType()){
            case ADMIN_RESET ->  PasswordChangeRequirementType.ADMIN_RESET;
            case INITIAL_REGISTER ->   PasswordChangeRequirementType.INITIAL_REGISTER;
            case USER_CHANGE -> {
                ExpiredPasswordPolicy policy = new ExpiredPasswordPolicy(LocalDateTime.now());
                if(policy.isExpired(last.changedAt())){
                    yield PasswordChangeRequirementType.EXPIRED;
                }
                yield PasswordChangeRequirementType.NONE;
            }
        };


    }

    @Override
    @Transactional
    public void changePassword(AuthUserId userId, String currentRawPassword, String newRawPassword) {

        AuthUser user = authUserRepository.findById(userId)
                .orElseThrow(() -> new AuthDomainException("ユーザID未採番のためパスワード変更ができません。"));

        // 現在のパスワード検証
        if (!passwordEncoder.matches(currentRawPassword, user.encodedPassword().value())) {
            throw new AuthDomainException("現在のパスワードが正しくありません。");
        }

        // パスワード構文チェック
        passwordPolicy.validatePassword(newRawPassword, user.loginId());

        // 履歴による再利用禁止チェック（直近 N 件）
        List<PasswordHistory> recentHistories =
                passwordHistoryRepository.findRecentByUserId(userId, 3);

        for (PasswordHistory history : recentHistories) {
            if (passwordEncoder.matches(newRawPassword, history.encodedPassword().value())) {
                throw new PasswordReuseNotAllowedException();
            }
        }

        // 新しいパスワードをハッシュ化
        String encoded = passwordEncoder.encode(newRawPassword);
        EncodedPassword encodedPassword = new EncodedPassword(encoded);

        // ユーザのパスワード更新
        user.changePassword(encodedPassword);
        authUserRepository.save(user);

        // パスワード履歴登録
        LocalDateTime now = LocalDateTime.now();
        LoginId operator = user.loginId(); // 自分自身が変更
        PasswordHistory history = PasswordHistory.userChange(userId, encodedPassword, now, operator);
        passwordHistoryRepository.save(history);
    }


}
