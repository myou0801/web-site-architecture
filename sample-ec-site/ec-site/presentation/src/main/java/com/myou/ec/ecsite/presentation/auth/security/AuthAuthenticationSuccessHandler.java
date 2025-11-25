package com.myou.ec.ecsite.presentation.auth.security;

import com.myou.ec.ecsite.application.auth.sharedservice.LoginProcessSharedService;
import com.myou.ec.ecsite.domain.auth.exception.AuthDomainException;
import com.myou.ec.ecsite.domain.auth.model.value.LoginId;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * ログイン成功時のハンドラ（presentation層）。
 * HTTP / URL のみを扱い、ドメイン処理は LoginProcessSharedService に委譲する。
 */
@Component
public class AuthAuthenticationSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    public static final String SESSION_ATTR_PASSWORD_CHANGE_REQUIRED = "AUTH_PASSWORD_CHANGE_REQUIRED";

    private final LoginProcessSharedService loginProcessSharedService;

    // デフォルト遷移先URL（必要に応じて設定で差し替え可能にしてもよい）
    private final String defaultMenuUrl = "/menu";
    private final String passwordChangeUrl = "/.well-known/change-password";

    public AuthAuthenticationSuccessHandler(LoginProcessSharedService loginProcessSharedService) {
        this.loginProcessSharedService = loginProcessSharedService;
        setDefaultTargetUrl(defaultMenuUrl);
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication)
            throws IOException, ServletException {

        String loginIdStr = extractLoginId(authentication);
        LoginId loginId = new LoginId(loginIdStr);
        boolean result = loginProcessSharedService.isPasswordChangeRequired(loginId);

        if (result) {
            // パスワード変更画面へリダイレクト
            clearAuthenticationAttributes(request);
            getRedirectStrategy().sendRedirect(request, response, passwordChangeUrl);
        } else {
            super.onAuthenticationSuccess(request, response, authentication);
        }
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

