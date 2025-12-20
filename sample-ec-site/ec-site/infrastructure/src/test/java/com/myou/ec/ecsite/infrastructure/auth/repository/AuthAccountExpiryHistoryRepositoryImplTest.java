package com.myou.ec.ecsite.infrastructure.auth.repository;

import com.myou.ec.ecsite.domain.auth.model.AccountExpiryEvent;
import com.myou.ec.ecsite.domain.auth.model.AccountExpiryEvents;
import com.myou.ec.ecsite.domain.auth.model.value.AccountExpiryEventType;
import com.myou.ec.ecsite.domain.auth.model.value.AuthAccountId;
import com.myou.ec.ecsite.domain.auth.model.value.Operator;
import com.myou.ec.ecsite.infrastructure.auth.mapper.AuthAccountExpiryHistoryMapper;
import com.myou.ec.ecsite.infrastructure.auth.record.AccountExpiryHistoryRecord;
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

import static org.assertj.core.api.Assertions.assertThat;

@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
class AuthAccountExpiryHistoryRepositoryImplTest {

    private AuthAccountExpiryHistoryRepositoryImpl authAccountExpiryHistoryRepository;

    @Autowired
    private AuthAccountExpiryHistoryMapper authAccountExpiryHistoryMapper;

    private Clock clock;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(Instant.parse("2023-01-01T10:00:00Z"), ZoneId.systemDefault());
        authAccountExpiryHistoryRepository = new AuthAccountExpiryHistoryRepositoryImpl(authAccountExpiryHistoryMapper, clock);
    }

    @Nested
    @DisplayName("save")
    class Save {
        @Test
        @DisplayName("アカウント失効イベントを正常に保存できること")
        void testSave() {
            AuthAccountId accountId = new AuthAccountId(1L);
            AccountExpiryEvent event = AccountExpiryEvent.unexpired(accountId, "test", LocalDateTime.now(clock), Operator.system());;
            Operator operator = Operator.system();

            authAccountExpiryHistoryRepository.save(event, operator);

            var records = authAccountExpiryHistoryMapper.selectByAccountId(accountId.value());
            assertThat(records).hasSize(1);
            AccountExpiryHistoryRecord savedRecord = records.get(0);
            assertThat(savedRecord.authAccountId()).isEqualTo(accountId.value()); // getAuthAccountId
            assertThat(savedRecord.eventType()).isEqualTo(AccountExpiryEventType.UNEXPIRE.name()); // getExpiryReason
            assertThat(savedRecord.createdBy()).isEqualTo(operator.value()); // getCreatedBy
        }
    }

    @Nested
    @DisplayName("findByAccountId")
    class FindByAccountId {
        @Test
        @DisplayName("指定したアカウントIDの失効履歴を取得できること")
        void testFindByAccountId_found() {
            AuthAccountId accountId = new AuthAccountId(1L);
            AccountExpiryEvent event = AccountExpiryEvent.expired(accountId, "test", LocalDateTime.now(clock), Operator.system());;
            authAccountExpiryHistoryRepository.save(event, Operator.system());

            AccountExpiryEvents result = authAccountExpiryHistoryRepository.findByAccountId(accountId);

            assertThat(result.isExpired()).isTrue();

        }

        @Test
        @DisplayName("失効履歴が存在しないアカウントの場合に空のリストが返されること")
        void testFindByAccountId_notFound() {
            AuthAccountId accountId = new AuthAccountId(99L); // non-existent
            AccountExpiryEvents result = authAccountExpiryHistoryRepository.findByAccountId(accountId);

        }
    }
}
