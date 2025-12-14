package com.myou.ec.ecsite.presentation.auth.security.userdetails;

import com.myou.ec.ecsite.application.auth.sharedservice.AccountExpirySharedService;
import com.myou.ec.ecsite.domain.auth.model.AccountLockEvents;
import com.myou.ec.ecsite.domain.auth.model.AuthAccount;
import com.myou.ec.ecsite.domain.auth.model.LoginHistory;
import com.myou.ec.ecsite.domain.auth.model.value.AuthAccountId;
import com.myou.ec.ecsite.domain.auth.model.value.UserId;
import com.myou.ec.ecsite.domain.auth.repository.AuthAccountLockHistoryRepository;
import com.myou.ec.ecsite.domain.auth.repository.AuthAccountRepository;
import com.myou.ec.ecsite.domain.auth.repository.AuthAccountRoleRepository;
import com.myou.ec.ecsite.domain.auth.repository.AuthLoginHistoryRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuthAccountDetailsService implements UserDetailsService {

    private final AuthAccountRepository authAccountRepository;
    private final AuthAccountRoleRepository authAccountRoleRepository;
    private final AuthAccountLockHistoryRepository lockHistoryRepository;
    private final AuthLoginHistoryRepository loginHistoryRepository;
    private final AccountExpirySharedService accountExpirySharedService;
    private final Clock clock;


    public AuthAccountDetailsService(AuthAccountRepository authAccountRepository,
                                     AuthAccountRoleRepository authAccountRoleRepository,
                                     AuthAccountLockHistoryRepository lockHistoryRepository,
                                     AuthLoginHistoryRepository loginHistoryRepository,
                                     AccountExpirySharedService accountExpirySharedService,
                                     Clock clock) {
        this.authAccountRepository = authAccountRepository;
        this.authAccountRoleRepository = authAccountRoleRepository;
        this.lockHistoryRepository = lockHistoryRepository;
        this.loginHistoryRepository = loginHistoryRepository;
        this.accountExpirySharedService = accountExpirySharedService;
        this.clock = clock;
    }

    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

        UserId userId = new UserId(username);
        AuthAccount user = authAccountRepository.findByUserId(userId)
                .orElseThrow(() -> new UsernameNotFoundException("アカウントが存在しません: " + username));

        AuthAccountId accountId = user.id();

        // 前回ログイン日時（今回ログインより前の SUCCESS）
        LocalDateTime previousLoginAt = loginHistoryRepository
                .findLatestSuccessByAccountId(accountId)
                .map(LoginHistory::loginAt)
                .orElse(null);

        // 休眠状態かの判定
        boolean expired = accountExpirySharedService.evaluateAndExpireIfNeeded(accountId, userId);

        // ロック状態の判定
        AccountLockEvents lockEvents = lockHistoryRepository.findByAccountId(accountId, 20);
        boolean locked = lockEvents.isLocked();


        List<SimpleGrantedAuthority> authorities = authAccountRoleRepository.findRolesByAccountId(accountId).stream()
                .map(rc -> new SimpleGrantedAuthority("ROLE_" + rc.value()))
                .toList();

        boolean enabled = user.canLogin();     // 無効フラグ等を見ている想定
        boolean accountNonExpired = !expired;
        boolean credentialsNonExpired = true;
        boolean accountNonLocked = !locked;

        return new AuthAccountDetails(
                accountId,
                user.userId().value(),
                user.passwordHash().value(),
                enabled,
                accountNonExpired,
                credentialsNonExpired,
                accountNonLocked,
                authorities,
                previousLoginAt
        );
    }

}
