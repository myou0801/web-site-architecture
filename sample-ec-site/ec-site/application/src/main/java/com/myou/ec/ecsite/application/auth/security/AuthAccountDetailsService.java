package com.myou.ec.ecsite.application.auth.security;

import com.myou.ec.ecsite.domain.auth.model.AccountLockEvents;
import com.myou.ec.ecsite.domain.auth.model.AuthAccount;
import com.myou.ec.ecsite.domain.auth.model.LoginHistory;
import com.myou.ec.ecsite.domain.auth.model.value.AuthAccountId;
import com.myou.ec.ecsite.domain.auth.model.value.UserId;
import com.myou.ec.ecsite.domain.auth.repository.AuthAccountLockHistoryRepository;
import com.myou.ec.ecsite.domain.auth.repository.AuthAccountRepository;
import com.myou.ec.ecsite.domain.auth.repository.AuthLoginHistoryRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuthAccountDetailsService implements UserDetailsService {

    private final AuthAccountRepository authAccountRepository;
    private final AuthAccountLockHistoryRepository lockHistoryRepository;
    private final AuthLoginHistoryRepository loginHistoryRepository;

    public AuthAccountDetailsService(AuthAccountRepository authAccountRepository,
                                     AuthAccountLockHistoryRepository lockHistoryRepository,
                                     AuthLoginHistoryRepository loginHistoryRepository) {
        this.authAccountRepository = authAccountRepository;
        this.lockHistoryRepository = lockHistoryRepository;
        this.loginHistoryRepository = loginHistoryRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

        UserId userId = new UserId(username);
        AuthAccount user = authAccountRepository.findByUserId(userId)
                .orElseThrow(() -> new UsernameNotFoundException("アカウントが存在しません: " + username));

        AuthAccountId accountId = user.id();

        // ロック状態の判定
        boolean locked = false;
        if (accountId != null) {
            AccountLockEvents lockEvents = lockHistoryRepository.findByAccountId(accountId, 20);
            locked = lockEvents.isLocked();
        }

        // 前回ログイン日時（今回ログインより前の SUCCESS）
        LocalDateTime previousLoginAt = null;
        if (accountId != null) {
            previousLoginAt = loginHistoryRepository
                    .findPreviousSuccessLoginAtByAccountId(accountId)
                    .map(LoginHistory::loginAt)
                    .orElse(null);
        }

        List<SimpleGrantedAuthority> authorities = user.roleCodes().stream()
                .map(rc -> new SimpleGrantedAuthority(rc.value()))
                .toList();

        boolean enabled = user.canLogin();     // 無効フラグ等を見ている想定
        boolean accountNonExpired = true;
        boolean credentialsNonExpired = true;
        boolean accountNonLocked = !locked;

        return new AuthAccountDetails(
                accountId,
                user.userId().value(),
                user.encodedPassword().value(),
                enabled,
                accountNonExpired,
                credentialsNonExpired,
                accountNonLocked,
                authorities,
                previousLoginAt
        );
    }
}
