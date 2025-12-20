package com.myou.ec.ecsite.presentation.auth.security.handler;

import com.myou.ec.ecsite.application.auth.sharedservice.PasswordChangeSharedService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * ログイン成功時のハンドラ（presentation層）。
 * HTTP / URL のみを扱い、ドメイン処理は LoginProcessSharedService に委譲する。
 */
@Component
public class AuthAuthenticationSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    //public static final String SESSION_ATTR_PASSWORD_CHANGE_REQUIRED = "AUTH_PASSWORD_CHANGE_REQUIRED";

    private final PasswordChangeSharedService passwordChangeSharedService;

    // デフォルト遷移先URL（必要に応じて設定で差し替え可能にしてもよい）
//    private final String defaultMenuUrl = "/menu";
    private static final String passwordChangeUrl = "/.well-known/change-password";

    public AuthAuthenticationSuccessHandler(PasswordChangeSharedService passwordChangeSharedService) {
        this.passwordChangeSharedService = passwordChangeSharedService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication)
            throws IOException, ServletException {

        boolean result = passwordChangeSharedService.isPasswordChangeRequired();

        if (result) {
            // パスワード変更画面へリダイレクト
            getRedirectStrategy().sendRedirect(request, response, passwordChangeUrl);
        } else {
            super.onAuthenticationSuccess(request, response, authentication);
        }
    }

}

