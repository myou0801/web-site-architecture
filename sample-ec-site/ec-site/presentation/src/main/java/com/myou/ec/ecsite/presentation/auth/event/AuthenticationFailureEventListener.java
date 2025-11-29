package com.myou.ec.ecsite.presentation.auth.event;

import com.myou.ec.ecsite.application.auth.sharedservice.LoginProcessSharedService;
import com.myou.ec.ecsite.domain.auth.exception.AuthDomainException;
import com.myou.ec.ecsite.domain.auth.model.value.UserId;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;

@Component
public class AuthenticationFailureEventListener {

    private final LoginProcessSharedService loginProcessSharedService;

    public AuthenticationFailureEventListener(LoginProcessSharedService loginProcessSharedService) {
        this.loginProcessSharedService = loginProcessSharedService;
    }

    @EventListener
    public void handle(AbstractAuthenticationFailureEvent event) {
        Authentication authentication = event.getAuthentication();
        String loginIdStr = extractLoginId(authentication);
        UserId loginId = new UserId(loginIdStr);
        loginProcessSharedService.onLoginFailure(loginId);
    }

    private String extractLoginId(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof User userDetails) {
            return userDetails.getUsername();
        }
        if (principal instanceof String s) {
            return s;
        }
        throw new AuthDomainException("認証情報からログインIDを取得できません。");
    }
}

