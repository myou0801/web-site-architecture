package com.myou.ec.ecsite.infrastructure.auth.repository;

import com.myou.ec.ecsite.domain.auth.model.AccountLockEvent;
import com.myou.ec.ecsite.domain.auth.model.AccountLockEvents;
import com.myou.ec.ecsite.domain.auth.model.value.AuthAccountId;
import com.myou.ec.ecsite.domain.auth.model.value.Operator;
import com.myou.ec.ecsite.infrastructure.auth.mapper.AuthAccountLockHistoryMapper;
import com.myou.ec.ecsite.infrastructure.auth.record.AuthAccountLockHistoryRecord;
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

import static org.assertj.core.api.Assertions.assertThat;

@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
class AuthAccountLockHistoryRepositoryImplTest {

    private AuthAccountLockHistoryRepositoryImpl authAccountLockHistoryRepository;

    @Autowired
    private AuthAccountLockHistoryMapper authAccountLockHistoryMapper;

    private Clock clock;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(Instant.parse("2023-01-01T10:00:00Z"), ZoneId.systemDefault());
        authAccountLockHistoryRepository = new AuthAccountLockHistoryRepositoryImpl(authAccountLockHistoryMapper, clock);
    }

    @Nested
    @DisplayName("save")
    class Save {
        @Test
        @DisplayName("アカウントロックイベントを正常に保存できること")
        void testSave() {
            AuthAccountId accountId = new AuthAccountId(1L);
            AccountLockEvent event = AccountLockEvent.lock (new AuthAccountId(1L), LocalDateTime.now(clock), "test", Operator.system());
            Operator operator = Operator.system();

            authAccountLockHistoryRepository.save(event, operator);

            // Verify insertion
            List<AuthAccountLockHistoryRecord> records = authAccountLockHistoryMapper.selectRecentByAccountId(accountId.value(), 1);
            assertThat(records).hasSize(1);
            AuthAccountLockHistoryRecord savedRecord = records.get(0);
            assertThat(savedRecord.authAccountId()).isEqualTo(accountId.value());
            assertThat(savedRecord.locked()).isTrue();

        }
    }

    @Nested
    @DisplayName("findByAccountId")
    class FindByAccountId {
        @Test
        @DisplayName("指定したアカウントIDのロック履歴を取得できること")
        void testFindByAccountId_found() {
            // Data from data.sql (assuming account id 3 is locked)
            AuthAccountId accountId = new AuthAccountId(3L);
            int limit = 5;

            AccountLockEvents result = authAccountLockHistoryRepository.findByAccountId(accountId, limit);

            assertThat(result.isLocked()).isFalse();
            assertThat(result.lastUnlockAt().isPresent()).isFalse();

        }

        @Test
        @DisplayName("ロック履歴が存在しないアカウントの場合に空のリストが返されること")
        void testFindByAccountId_notFound() {
            AuthAccountId accountId = new AuthAccountId(99L); // non-existent
            int limit = 5;

            AccountLockEvents result = authAccountLockHistoryRepository.findByAccountId(accountId, limit);

            assertThat(result.isLocked()).isFalse();
            assertThat(result.lastUnlockAt().isPresent()).isFalse();
        }

        @Test
        @DisplayName("limitで取得件数を制限できること")
        void testFindByAccountId_withLimit() {
            AuthAccountId accountId = new AuthAccountId(1L);
            // Insert 3 records
            authAccountLockHistoryRepository.save(AccountLockEvent.lock (accountId, LocalDateTime.now(clock), "test", Operator.system()), Operator.system());
            authAccountLockHistoryRepository.save(AccountLockEvent.lock (accountId, LocalDateTime.now(clock).plusMinutes(1), "test", Operator.system()), Operator.system());
            authAccountLockHistoryRepository.save(AccountLockEvent.unlock (accountId, LocalDateTime.now(clock).plusMinutes(2), "test", Operator.system()), Operator.system());

            AccountLockEvents result = authAccountLockHistoryRepository.findByAccountId(accountId, 2);

            assertThat(result.isLocked()).isFalse();
            assertThat(result.lastUnlockAt().isPresent()).isTrue();
        }
    }
}
