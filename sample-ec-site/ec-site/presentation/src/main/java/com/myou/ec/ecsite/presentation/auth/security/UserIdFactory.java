package com.myou.ec.ecsite.presentation.auth.security;

import com.myou.ec.ecsite.domain.auth.exception.AuthDomainException;
import com.myou.ec.ecsite.domain.auth.model.value.UserId;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;

public class UserIdFactory {

    public static UserId create(Authentication authentication){
        Object principal = authentication.getPrincipal();
        if (principal instanceof User userDetails) {
            return new UserId(userDetails.getUsername()) ;
        }
        if (principal instanceof String s) {
            return new UserId(s);
        }
        throw new AuthDomainException("認証情報からログインIDを取得できません。");
    }

}
