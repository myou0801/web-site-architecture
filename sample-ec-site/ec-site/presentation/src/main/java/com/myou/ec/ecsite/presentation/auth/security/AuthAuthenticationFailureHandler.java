package com.myou.ec.ecsite.presentation.auth.security;

import com.myou.ec.ecsite.application.auth.sharedservice.LoginFailureType;
import com.myou.ec.ecsite.application.auth.sharedservice.LoginProcessSharedService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * ログイン失敗時のハンドラ（presentation層）。
 * HTTP / URL のみを扱い、ドメイン処理は LoginProcessSharedService に委譲する。
 */
@Component
public class AuthAuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    public static final String SESSION_ATTR_AUTH_ERROR_TYPE = "AUTH_ERROR_TYPE";
    public static final String ERROR_TYPE_LOCKED = "LOCKED";
    public static final String ERROR_TYPE_BAD_CREDENTIALS = "BAD_CREDENTIALS";

    private final LoginProcessSharedService loginProcessSharedService;

    // ログイン画面 URL
    private final String loginUrl = "/login?error";

    public AuthAuthenticationFailureHandler(LoginProcessSharedService loginProcessSharedService) {
        this.loginProcessSharedService = loginProcessSharedService;
        setDefaultFailureUrl(loginUrl);
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception)
            throws IOException, ServletException {

        // フォームの name="username" としている前提。違う場合はここを調整。
        String loginId = request.getParameter("username");

        LoginFailureType type =
                loginProcessSharedService.onLoginFailure(loginId);

        String errorTypeStr = switch (type) {
            case LOCKED -> ERROR_TYPE_LOCKED;
            case BAD_CREDENTIALS -> ERROR_TYPE_BAD_CREDENTIALS;
        };

        request.getSession(true)
               .setAttribute(SESSION_ATTR_AUTH_ERROR_TYPE, errorTypeStr);

        super.onAuthenticationFailure(request, response, exception);
    }
}
