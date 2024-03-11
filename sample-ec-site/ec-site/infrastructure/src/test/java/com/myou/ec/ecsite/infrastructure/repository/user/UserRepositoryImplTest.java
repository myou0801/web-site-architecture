package com.myou.ec.ecsite.infrastructure.repository.user;

import com.myou.ec.ecsite.domain.model.user.MailAddress;
import com.myou.ec.ecsite.domain.model.user.User;
import com.myou.ec.ecsite.domain.model.user.UserName;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.jdbc.JdbcTestUtils;


@DataJdbcTest
class UserRepositoryImplTest {

    @Autowired
    private UserJdbcRepository userJdbcRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
    }

    @Test
    void register() {
        Assertions.assertThat(JdbcTestUtils.countRowsInTable(jdbcTemplate,"\"USER\"")).isEqualTo(0) ;
        UserRepositoryImpl target = new UserRepositoryImpl(userJdbcRepository);
        target.register(User.createNewUser(new UserName("test"), new MailAddress("test@mail.com")));
        Assertions.assertThat(JdbcTestUtils.countRowsInTable(jdbcTemplate,"\"USER\"")).isEqualTo(1) ;
    }

}
