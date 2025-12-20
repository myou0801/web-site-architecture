package com.myou.ec.ecsite.infrastructure.auth.repository;

import com.myou.ec.ecsite.domain.auth.model.PasswordHistory;
import com.myou.ec.ecsite.domain.auth.model.value.AuthAccountId;
import com.myou.ec.ecsite.domain.auth.model.value.Operator;
import com.myou.ec.ecsite.domain.auth.model.value.PasswordHash;
import com.myou.ec.ecsite.infrastructure.auth.mapper.AuthPasswordHistoryMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
class AuthPasswordHistoryRepositoryImplTest {

    private AuthPasswordHistoryRepositoryImpl authPasswordHistoryRepository;

    @Autowired
    private AuthPasswordHistoryMapper authPasswordHistoryMapper;
    
    private Clock clock;

    @BeforeEach
    void setUp() {
        authPasswordHistoryRepository = new AuthPasswordHistoryRepositoryImpl(authPasswordHistoryMapper);
        clock = Clock.fixed(Instant.parse("2023-01-01T10:00:00Z"), ZoneId.systemDefault());
    }


    @Nested
    @DisplayName("save")
    class Save {
        @Test
        @DisplayName("パスワード履歴を正常に保存できること")
        void testSave() {
            AuthAccountId accountId = new AuthAccountId(1L);
            PasswordHistory history = PasswordHistory.userChange(new AuthAccountId(1L), new PasswordHash("pass"), LocalDateTime.now(clock), Operator.system());
            Operator operator = Operator.of("test-user");

            authPasswordHistoryRepository.save(history, operator);

            // Verify insertion
            List<PasswordHistory> savedHistories = authPasswordHistoryRepository.findRecentByAccountId(accountId, 1);
            assertThat(savedHistories).hasSize(1);
            assertThat(savedHistories.get(0).passwordHash().value()).isEqualTo("pass");
        }
    }

    @Nested
    @DisplayName("findRecentByAccountId")
    class FindRecentByAccountId {
        @Test
        @DisplayName("指定アカウントの最近のパスワード履歴を取得できること")
        void testFindRecent_found() {
            // from data.sql
            AuthAccountId accountId = new AuthAccountId(4L); 
            List<PasswordHistory> histories = authPasswordHistoryRepository.findRecentByAccountId(accountId, 5);

            assertThat(histories).hasSize(3);
            assertThat(histories.get(0).passwordHash().value()).isEqualTo("expired-pass3");
        }

        @Test
        @DisplayName("limitで取得件数を制限できること")
        void testFindRecent_withLimit() {
            AuthAccountId accountId = new AuthAccountId(4L);
            List<PasswordHistory> histories = authPasswordHistoryRepository.findRecentByAccountId(accountId, 2);

            assertThat(histories).hasSize(2);
            assertThat(histories.get(0).passwordHash().value()).isEqualTo("expired-pass3");
            assertThat(histories.get(1).passwordHash().value()).isEqualTo("expired-pass2");
        }
    }

    @Nested
    @DisplayName("findLastByAccountId")
    class FindLastByAccountId {
        @Test
        @DisplayName("指定アカウントの最後のパスワード履歴を取得できること")
        void testFindLast_found() {
            AuthAccountId accountId = new AuthAccountId(4L);
            Optional<PasswordHistory> history = authPasswordHistoryRepository.findLastByAccountId(accountId);

            assertThat(history).isPresent();
            assertThat(history.get().passwordHash().value()).isEqualTo("expired-pass3");
        }

        @Test
        @DisplayName("履歴がない場合にOptional.emptyが返されること")
        void testFindLast_notFound() {
            AuthAccountId accountId = new AuthAccountId(99L);
            Optional<PasswordHistory> history = authPasswordHistoryRepository.findLastByAccountId(accountId);

            assertThat(history).isEmpty();
        }
    }
}
