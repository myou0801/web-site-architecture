package com.myou.ec.ecsite.application.auth.sharedservice;

import com.myou.ec.ecsite.domain.auth.exception.AuthDomainException;
import com.myou.ec.ecsite.domain.auth.exception.PasswordReuseNotAllowedException;
import com.myou.ec.ecsite.domain.auth.model.AuthAccount;
import com.myou.ec.ecsite.domain.auth.model.PasswordHistory;
import com.myou.ec.ecsite.domain.auth.model.value.AuthAccountId;
import com.myou.ec.ecsite.domain.auth.model.value.PasswordHash;
import com.myou.ec.ecsite.domain.auth.model.value.UserId;
import com.myou.ec.ecsite.domain.auth.policy.ExpiredPasswordPolicy;
import com.myou.ec.ecsite.domain.auth.policy.PasswordPolicy;
import com.myou.ec.ecsite.domain.auth.repository.AuthAccountRepository;
import com.myou.ec.ecsite.domain.auth.repository.AuthPasswordHistoryRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class PasswordChangeSharedServiceImpl implements PasswordChangeSharedService {

    private final AuthAccountRepository authAccountRepository;
    private final AuthPasswordHistoryRepository passwordHistoryRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicy passwordPolicy;
    private final Clock clock;

    public PasswordChangeSharedServiceImpl(AuthAccountRepository authAccountRepository,
                                           AuthPasswordHistoryRepository passwordHistoryRepository,
                                           PasswordEncoder passwordEncoder,
                                           PasswordPolicy passwordPolicy, Clock clock) {
        this.authAccountRepository = authAccountRepository;
        this.passwordHistoryRepository = passwordHistoryRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicy = passwordPolicy;
        this.clock = clock;
    }



    @Override
    @Transactional(readOnly = true)
    public PasswordChangeRequirementType requirementOf(UserId userId) {

        Optional<PasswordHistory> optLast = authAccountRepository.findByUserId(userId)
                .flatMap(user -> passwordHistoryRepository.findLastByAccountId(user.id()));

        if (optLast.isEmpty()) {
            return PasswordChangeRequirementType.INITIAL_REGISTER;
        }

        PasswordHistory last = optLast.get();
        return switch (last.changeType()){
            case ADMIN_RESET ->  PasswordChangeRequirementType.ADMIN_RESET;
            case INITIAL_REGISTER ->   PasswordChangeRequirementType.INITIAL_REGISTER;
            case USER_CHANGE -> {
                ExpiredPasswordPolicy policy = new ExpiredPasswordPolicy(last.changedAt());
                if(policy.isExpired(LocalDateTime.now(clock))){
                    yield PasswordChangeRequirementType.EXPIRED;
                }
                yield PasswordChangeRequirementType.NONE;
            }
        };


    }

    @Override
    @Transactional
    public void changePassword(AuthAccountId accountId, String currentRawPassword, String newRawPassword) {

        AuthAccount user = authAccountRepository.findById(accountId)
                .orElseThrow(() -> new AuthDomainException("アカウントID未採番のためパスワード変更ができません。"));

        // 現在のパスワード検証
        if (!passwordEncoder.matches(currentRawPassword, user.passwordHash().value())) {
            throw new AuthDomainException("現在のパスワードが正しくありません。");
        }

        // パスワード構文チェック
        passwordPolicy.validatePassword(newRawPassword, user.userId());

        // 履歴による再利用禁止チェック（直近 N 件）
        List<PasswordHistory> recentHistories =
                passwordHistoryRepository.findRecentByAccountId(accountId, 3);

        for (PasswordHistory history : recentHistories) {
            if (passwordEncoder.matches(newRawPassword, history.passwordHash().value())) {
                throw new PasswordReuseNotAllowedException();
            }
        }

        // 新しいパスワードをハッシュ化
        String encoded = passwordEncoder.encode(newRawPassword);
        PasswordHash passwordHash = new PasswordHash(encoded);

        UserId operator = user.userId(); // 自分自身が変更
        LocalDateTime now = LocalDateTime.now(clock);

        // ユーザのパスワード更新
        authAccountRepository.save(user.changePassword(passwordHash), operator);

        // パスワード履歴登録


        PasswordHistory history = PasswordHistory.userChange(accountId, passwordHash, now, operator);
        passwordHistoryRepository.save(history, operator);
    }


}
