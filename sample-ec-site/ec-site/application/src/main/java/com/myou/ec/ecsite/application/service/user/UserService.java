package com.myou.ec.ecsite.application.service.user;

import com.myou.ec.ecsite.domain.model.user.User;

import java.util.List;

public interface UserService {

    void registerUser(User user);

    List<User> findAllUsers();

}
