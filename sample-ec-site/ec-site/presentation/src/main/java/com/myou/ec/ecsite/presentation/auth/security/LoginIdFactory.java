package com.myou.ec.ecsite.presentation.auth.security;

import com.myou.ec.ecsite.domain.auth.exception.AuthDomainException;
import com.myou.ec.ecsite.domain.auth.model.value.LoginId;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;

public class LoginIdFactory {

    public static LoginId create(Authentication authentication){
        Object principal = authentication.getPrincipal();
        if (principal instanceof User userDetails) {
            return new LoginId(userDetails.getUsername()) ;
        }
        if (principal instanceof String s) {
            return new LoginId(s);
        }
        throw new AuthDomainException("認証情報からログインIDを取得できません。");
    }

}
