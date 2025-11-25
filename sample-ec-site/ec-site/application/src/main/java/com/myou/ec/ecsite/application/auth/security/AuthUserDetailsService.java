package com.myou.ec.ecsite.application.auth.security;

import com.myou.ec.ecsite.domain.auth.model.AccountLockEvents;
import com.myou.ec.ecsite.domain.auth.model.AuthUser;
import com.myou.ec.ecsite.domain.auth.model.LoginHistory;
import com.myou.ec.ecsite.domain.auth.model.value.AuthUserId;
import com.myou.ec.ecsite.domain.auth.model.value.LoginId;
import com.myou.ec.ecsite.domain.auth.repository.AuthAccountLockHistoryRepository;
import com.myou.ec.ecsite.domain.auth.repository.AuthLoginHistoryRepository;
import com.myou.ec.ecsite.domain.auth.repository.AuthUserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuthUserDetailsService implements UserDetailsService {

    private final AuthUserRepository authUserRepository;
    private final AuthAccountLockHistoryRepository lockHistoryRepository;
    private final AuthLoginHistoryRepository loginHistoryRepository;

    public AuthUserDetailsService(AuthUserRepository authUserRepository,
                                  AuthAccountLockHistoryRepository lockHistoryRepository,
                                  AuthLoginHistoryRepository loginHistoryRepository) {
        this.authUserRepository = authUserRepository;
        this.lockHistoryRepository = lockHistoryRepository;
        this.loginHistoryRepository = loginHistoryRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

        LoginId loginId = new LoginId(username);
        AuthUser user = authUserRepository.findByLoginId(loginId)
                .orElseThrow(() -> new UsernameNotFoundException("ユーザが存在しません: " + username));

        AuthUserId userId = user.id();

        // ロック状態の判定
        boolean locked = false;
        if (userId != null) {
            AccountLockEvents lockEvents = lockHistoryRepository.findByUserId(userId);
            locked = lockEvents.isLocked();
        }

        // 前回ログイン日時（今回ログインより前の SUCCESS）
        LocalDateTime previousLoginAt = null;
        if (userId != null) {
            previousLoginAt = loginHistoryRepository
                    .findPreviousSuccessLoginAt(userId)
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

        return new AuthUserDetails(
                userId,
                user.loginId().value(),
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
