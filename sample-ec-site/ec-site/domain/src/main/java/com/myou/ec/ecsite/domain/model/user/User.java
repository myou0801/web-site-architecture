package com.myou.ec.ecsite.domain.model.user;

public record User(Long id, UserName userName, MailAddress mailAddress) {

    public static User createNewUser(UserName userName, MailAddress mailAddress) {
        return new User(null, userName, mailAddress);
    }

}
