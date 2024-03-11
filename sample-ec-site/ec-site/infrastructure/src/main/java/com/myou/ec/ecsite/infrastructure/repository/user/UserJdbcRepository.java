package com.myou.ec.ecsite.infrastructure.repository.user;

import org.springframework.data.repository.ListCrudRepository;

public interface UserJdbcRepository  extends ListCrudRepository<UserEntity, Long> {
}
