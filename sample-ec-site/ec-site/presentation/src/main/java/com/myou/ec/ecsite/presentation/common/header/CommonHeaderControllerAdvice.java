package com.myou.ec.ecsite.presentation.common.header;

import com.myou.ec.ecsite.application.auth.security.AuthAccountDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.time.LocalDateTime;

@ControllerAdvice(annotations = Controller.class)
public class CommonHeaderControllerAdvice {

    @ModelAttribute("headerInfo")
    public HeaderInfo headerInfo() {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()
                || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }

        if (!(auth.getPrincipal() instanceof AuthAccountDetails userDetails)) {
            return null;
        }

        String loginId = userDetails.getUsername();
        LocalDateTime previousLoginAt = userDetails.previousLoginAt();

        return new HeaderInfo(
                loginId,
                null,            // displayName 等は必要に応じて拡張
                previousLoginAt
        );
    }
}
