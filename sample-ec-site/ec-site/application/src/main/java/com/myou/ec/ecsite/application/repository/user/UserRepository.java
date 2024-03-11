package com.myou.ec.ecsite.application.repository.user;

import com.myou.ec.ecsite.domain.model.user.User;

import java.util.List;

public interface UserRepository {

    void register(User user);

    List<User> findAll();

}
