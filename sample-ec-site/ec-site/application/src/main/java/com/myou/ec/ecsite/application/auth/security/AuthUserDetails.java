package com.myou.ec.ecsite.application.auth.security;

import com.myou.ec.ecsite.domain.auth.model.value.AuthUserId;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.time.LocalDateTime;
import java.util.Collection;

/**
 * 認証済みユーザ情報。
 * Spring Security の User に、ドメインの情報（userId 等）と前回ログイン日時を追加。
 */
public class AuthUserDetails extends User {

    private final AuthUserId authUserId;
    private final LocalDateTime previousLoginAt;

    public AuthUserDetails(AuthUserId authUserId,
                           String username,
                           String password,
                           boolean enabled,
                           boolean accountNonExpired,
                           boolean credentialsNonExpired,
                           boolean accountNonLocked,
                           Collection<? extends GrantedAuthority> authorities,
                           LocalDateTime previousLoginAt) {
        super(username, password, enabled, accountNonExpired, credentialsNonExpired, accountNonLocked, authorities);
        this.authUserId = authUserId;
        this.previousLoginAt = previousLoginAt;
    }

    public AuthUserId authUserId() {
        return authUserId;
    }

    public LocalDateTime previousLoginAt() {
        return previousLoginAt;
    }
}
