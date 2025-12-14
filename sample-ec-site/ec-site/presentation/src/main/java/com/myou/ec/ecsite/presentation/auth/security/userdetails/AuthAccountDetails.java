package com.myou.ec.ecsite.presentation.auth.security.userdetails;

import com.myou.ec.ecsite.domain.auth.model.value.AuthAccountId;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.time.LocalDateTime;
import java.util.Collection;

/**
 * 認証済みアカウント情報。
 * Spring Security の User に、ドメインの情報（accountId 等）と前回ログイン日時を追加。
 */
public class AuthAccountDetails extends User {

    private final AuthAccountId authAccountId;
    private final LocalDateTime previousLoginAt;

    public AuthAccountDetails(AuthAccountId authAccountId,
                           String username,
                           String password,
                           boolean enabled,
                           boolean accountNonExpired,
                           boolean credentialsNonExpired,
                           boolean accountNonLocked,
                           Collection<? extends GrantedAuthority> authorities,
                           LocalDateTime previousLoginAt) {
        super(username, password, enabled, accountNonExpired, credentialsNonExpired, accountNonLocked, authorities);
        this.authAccountId = authAccountId;
        this.previousLoginAt = previousLoginAt;
    }

    public AuthAccountId authAccountId() {
        return authAccountId;
    }

    public LocalDateTime previousLoginAt() {
        return previousLoginAt;
    }
}
