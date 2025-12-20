package com.myou.ec.ecsite.presentation.auth.security.interceptor;

import com.myou.ec.ecsite.application.auth.sharedservice.PasswordChangeSharedService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;

public class PasswordChangeRequiredInterceptor implements HandlerInterceptor {

    private final PasswordChangeSharedService passwordChangeSharedService;
    private final List<String> bypassPatterns;
    private final AntPathMatcher matcher = new AntPathMatcher();
    private static final String passwordChangeUrl = "/.well-known/change-password";

    public PasswordChangeRequiredInterceptor(
            PasswordChangeSharedService passwordChangeSharedService,
            List<String> bypassPatterns
    ) {
        this.passwordChangeSharedService = passwordChangeSharedService;
        this.bypassPatterns = List.copyOf(bypassPatterns);
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        String path = request.getRequestURI().substring(request.getContextPath().length());

        if (isBypassed(path)) {
            return true; // 無限ループ防止（外部設定で制御）
        }

        if (passwordChangeSharedService.isPasswordChangeRequired()) {
            response.sendRedirect(request.getContextPath() + passwordChangeUrl);
            return false;
        }
        return true;
    }

    private boolean isBypassed(String path) {
        return bypassPatterns.stream().anyMatch(p -> matcher.match(p.trim(), path));
    }

}
