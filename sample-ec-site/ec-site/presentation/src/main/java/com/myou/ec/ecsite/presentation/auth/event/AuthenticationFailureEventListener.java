package com.myou.ec.ecsite.presentation.auth.event;

import com.myou.ec.ecsite.application.auth.sharedservice.LoginProcessSharedService;
import com.myou.ec.ecsite.domain.auth.model.value.LoginId;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.core.Authentication;
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
        String loginIdStr = (authentication != null) ? authentication.getName() : null;

        LoginId loginId = (loginIdStr != null && !loginIdStr.isBlank()) ? new LoginId(loginIdStr) : null;
        loginProcessSharedService.onLoginFailure(loginId);
    }
}

