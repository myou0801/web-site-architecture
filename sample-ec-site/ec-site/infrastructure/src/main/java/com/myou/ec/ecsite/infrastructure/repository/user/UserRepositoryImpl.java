package com.myou.ec.ecsite.infrastructure.repository.user;

import com.myou.ec.ecsite.application.repository.user.UserRepository;
import com.myou.ec.ecsite.domain.model.user.User;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class UserRepositoryImpl implements UserRepository {

    private final UserJdbcRepository userJdbcRepository;

    public UserRepositoryImpl(UserJdbcRepository userJdbcRepository) {
        this.userJdbcRepository = userJdbcRepository;
    }

    @Override
    public void register(User user) {
        userJdbcRepository.save(UserEntity.from(user));
    }

    @Override
    public List<User> findAll() {
        return userJdbcRepository.findAll()
                .stream()
                .map(UserEntity::toUser)
                .toList();
    }
}
