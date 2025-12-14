package com.myou.ec.ecsite.presentation.auth.security.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Redirects back to login page with an error key.
 * - dormant / locked / disabled / invalid
 */
@Component
public class AuthAuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private final String defaultFailureUrl;

    public AuthAuthenticationFailureHandler(String defaultFailureUrl) {
        this.defaultFailureUrl = defaultFailureUrl;
        super.setDefaultFailureUrl(defaultFailureUrl);
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {

        String errorKey = "invalid";
        if (exception instanceof AccountExpiredException) {
            errorKey = "expired";
        } else if (exception instanceof LockedException) {
            errorKey = "locked";
        } else if (exception instanceof DisabledException) {
            errorKey = "disabled";
        }

        getRedirectStrategy().sendRedirect(request, response, defaultFailureUrl + "=" + errorKey);
    }
}
