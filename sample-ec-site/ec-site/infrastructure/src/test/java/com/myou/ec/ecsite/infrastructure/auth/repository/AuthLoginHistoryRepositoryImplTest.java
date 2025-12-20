package com.myou.ec.ecsite.infrastructure.auth.repository;

import com.myou.ec.ecsite.domain.auth.model.LoginHistories;
import com.myou.ec.ecsite.domain.auth.model.LoginHistory;
import com.myou.ec.ecsite.domain.auth.model.value.AuthAccountId;
import com.myou.ec.ecsite.domain.auth.model.value.LoginResult;
import com.myou.ec.ecsite.domain.auth.model.value.Operator;
import com.myou.ec.ecsite.infrastructure.auth.mapper.AuthLoginHistoryMapper;
import com.myou.ec.ecsite.infrastructure.auth.record.AuthLoginHistoryRecord;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
class AuthLoginHistoryRepositoryImplTest {

    private AuthLoginHistoryRepositoryImpl authLoginHistoryRepository;

    @Autowired
    private AuthLoginHistoryMapper authLoginHistoryMapper;

    private Clock clock;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(Instant.parse("2023-01-01T10:00:00Z"), ZoneId.systemDefault());
        authLoginHistoryRepository = new AuthLoginHistoryRepositoryImpl(authLoginHistoryMapper, clock);
    }


    @Nested
    @DisplayName("save")
    class Save {
        @Test
        @DisplayName("ログイン履歴を正常に保存できること")
        void testSave() {
            AuthAccountId accountId = new AuthAccountId(1L);
            LoginHistory history = LoginHistory.success(accountId, LocalDateTime.now(clock));
            Operator operator = Operator.system();

            authLoginHistoryRepository.save(history, operator);

            AuthLoginHistoryRecord savedRecord = authLoginHistoryMapper.selectLatestSuccessByAccountId(accountId.value());
            assertThat(savedRecord).isNotNull();
            assertThat(savedRecord.authAccountId()).isEqualTo(accountId.value());
            assertThat(savedRecord.result()).isEqualTo(LoginResult.SUCCESS.name());
            assertThat(savedRecord.createdBy()).isEqualTo(operator.value());
        }
    }

    @Nested
    @DisplayName("findRecentByAccountId")
    class FindRecentByAccountId {
        @Test
        @DisplayName("指定したアカウントIDの最近のログイン履歴を取得できること")
        void testFindRecentByAccountId_found() {
            AuthAccountId accountId = new AuthAccountId(1L);
            authLoginHistoryRepository.save(LoginHistory.success(accountId, LocalDateTime.now(clock)), Operator.system());
            authLoginHistoryRepository.save(LoginHistory.fail(accountId, LocalDateTime.now(clock).plusMinutes(1)), Operator.system());

            LoginHistories result = authLoginHistoryRepository.findRecentByAccountId(accountId, 5);

            assertThat(result.countConsecutiveFailuresSince(LocalDateTime.now(clock).minusDays(1))).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("findLatestSuccessByAccountId")
    class FindLatestSuccessByAccountId {
        @Test
        @DisplayName("最新の成功したログイン履歴を取得できること")
        void testFindLatestSuccess_found() {
            AuthAccountId accountId = new AuthAccountId(1L);
            LocalDateTime successTime = LocalDateTime.now(clock);
            authLoginHistoryRepository.save(LoginHistory.success(accountId, LocalDateTime.now(clock)), Operator.system());
            authLoginHistoryRepository.save(LoginHistory.fail(accountId, LocalDateTime.now(clock).plusMinutes(1)), Operator.system());
            authLoginHistoryRepository.save(LoginHistory.fail(accountId, LocalDateTime.now(clock).plusMinutes(2)), Operator.system());

            Optional<LoginHistory> result = authLoginHistoryRepository.findLatestSuccessByAccountId(accountId);

            assertThat(result).isPresent();
            assertThat(result.get().result()).isEqualTo(LoginResult.SUCCESS);

        }

        @Test
        @DisplayName("成功したログイン履歴がない場合にOptional.emptyが返されること")
        void testFindLatestSuccess_notFound() {
            AuthAccountId accountId = new AuthAccountId(1L);
            authLoginHistoryRepository.save(LoginHistory.fail(accountId, LocalDateTime.now(clock)), Operator.system());

            Optional<LoginHistory> result = authLoginHistoryRepository.findLatestSuccessByAccountId(accountId);

            assertThat(result).isEmpty();
        }
    }
}
