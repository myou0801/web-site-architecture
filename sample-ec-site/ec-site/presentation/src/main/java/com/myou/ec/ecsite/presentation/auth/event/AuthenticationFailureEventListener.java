package com.myou.ec.ecsite.presentation.auth.event;

import com.myou.ec.ecsite.application.auth.sharedservice.LoginProcessSharedService;
import com.myou.ec.ecsite.domain.auth.exception.AuthDomainException;
import com.myou.ec.ecsite.domain.auth.model.value.LoginId;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationFailureDisabledEvent;
import org.springframework.security.authentication.event.AuthenticationFailureLockedEvent;
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
    public void handle(AuthenticationFailureBadCredentialsEvent event) {
        Authentication authentication = event.getAuthentication();
        String loginIdStr = extractLoginId(authentication);
        LoginId loginId = new LoginId(loginIdStr);
        loginProcessSharedService.onLoginFailure(loginId);
    }

    @EventListener
    public void handle(AuthenticationFailureDisabledEvent event) {
        Authentication authentication = event.getAuthentication();
        String loginIdStr = extractLoginId(authentication);
        LoginId loginId = new LoginId(loginIdStr);
        loginProcessSharedService.onLoginFailure(loginId);
    }


    @EventListener
    public void handle(AuthenticationFailureLockedEvent event) {
        Authentication authentication = event.getAuthentication();
        String loginIdStr = extractLoginId(authentication);
        LoginId loginId = new LoginId(loginIdStr);
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

