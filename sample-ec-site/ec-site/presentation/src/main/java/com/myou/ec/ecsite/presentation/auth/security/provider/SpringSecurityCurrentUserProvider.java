package com.myou.ec.ecsite.presentation.auth.security.provider;

import com.myou.ec.ecsite.application.auth.provider.CurrentUserProvider;
import com.myou.ec.ecsite.domain.auth.model.value.Operator;
import com.myou.ec.ecsite.presentation.auth.security.userdetails.AuthAccountDetails;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class SpringSecurityCurrentUserProvider implements CurrentUserProvider {

    @Override
    public Optional<Operator> current() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null) {
            return Optional.empty();
        }
        if (!auth.isAuthenticated()) {
            return Optional.empty();
        }
        if (auth instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }

        Object principal = auth.getPrincipal();
        return switch (principal) {
            case null ->  Optional.empty();

            // 1) 独自Principal（推奨）
            case AuthAccountDetails p -> Optional.of(Operator.of(p.getUsername()));

            // 2) Spring標準のUserDetails
            case UserDetails userDetails -> {
                String username = userDetails.getUsername();
                if (username != null && !username.trim().isEmpty()) {
                    yield Optional.of(Operator.of(username));
                }
                yield Optional.empty();
            }


            // 3) principalが文字列のケース
            case String name -> {
                if (!name.trim().isEmpty() && !"anonymousUser".equals(name)) {
                    yield Optional.of(Operator.of(name));
                }
                yield Optional.empty();
            }

            // 4) その他：Authentication#getName を最後の手段として使う
            default -> {

                String name = auth.getName();
                if (name != null && !name.trim().isEmpty() && !"anonymousUser".equals(name)) {
                    yield Optional.of(Operator.of(name));
                }

                yield Optional.empty();
            }
        };

    }
}
