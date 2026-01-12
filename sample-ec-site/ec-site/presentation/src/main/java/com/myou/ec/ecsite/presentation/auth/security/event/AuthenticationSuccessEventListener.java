package com.myou.ec.ecsite.presentation.auth.security.event;

import com.myou.ec.ecsite.application.auth.sharedservice.LoginProcessSharedService;
import com.myou.ec.ecsite.domain.auth.model.value.LoginId;
import com.myou.ec.ecsite.presentation.auth.security.LoginIdFactory;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

@Component
public class AuthenticationSuccessEventListener {

    private final LoginProcessSharedService loginProcessSharedService;

    public AuthenticationSuccessEventListener(LoginProcessSharedService loginProcessSharedService) {
        this.loginProcessSharedService = loginProcessSharedService;
    }

    @EventListener
    public void handle(AuthenticationSuccessEvent event) {
        LoginId loginId = LoginIdFactory.create(event.getAuthentication());
        loginProcessSharedService.onLoginSuccess(loginId);
    }

}
