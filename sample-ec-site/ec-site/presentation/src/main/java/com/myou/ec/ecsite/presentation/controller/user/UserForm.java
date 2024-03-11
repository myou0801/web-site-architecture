package com.myou.ec.ecsite.presentation.controller.user;

import com.myou.ec.ecsite.domain.model.user.MailAddress;
import com.myou.ec.ecsite.domain.model.user.User;
import com.myou.ec.ecsite.domain.model.user.UserName;

@lombok.Data
public class UserForm {

    private String userName;

    private String mailAddress;

    public User toUser() {
        return User.createNewUser(
                new UserName(this.userName),
                new MailAddress(this.mailAddress));
    }
}
