package com.myou.ec.ecsite.application.service.user;

import com.myou.ec.ecsite.application.repository.user.UserRepository;
import com.myou.ec.ecsite.domain.model.user.User;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void registerUser(User user) {
        userRepository.register(user);
    }

    @Override
    public List<User> findAllUsers() {
        return userRepository.findAll();
    }
}
