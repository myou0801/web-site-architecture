package com.myou.ec.ecsite.infrastructure.auth.repository;

import com.myou.ec.ecsite.domain.auth.model.AuthAccount;
import com.myou.ec.ecsite.domain.auth.model.value.*;
import com.myou.ec.ecsite.infrastructure.auth.mapper.AuthAccountMapper;
import com.myou.ec.ecsite.infrastructure.auth.record.AuthAccountRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@MybatisTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Transactional
class AuthAccountRepositoryImplTest {

    private AuthAccountRepositoryImpl authAccountRepositoryImpl;

    @Autowired
    private AuthAccountMapper authAccountMapper;

    @BeforeEach
    void setUp(){
        authAccountRepositoryImpl = new AuthAccountRepositoryImpl(authAccountMapper, Clock.systemDefaultZone());
    }

    private AuthAccountRecord createAuthAccountRecord(Long id, String userIdValue) {
        return new AuthAccountRecord(
                id,
                userIdValue,
                "encodedPassword",
                "ACTIVE",
                LocalDateTime.of(2023, 1, 1, 0, 0, 0),
                "createdBy",
                LocalDateTime.of(2023, 1, 1, 0, 0, 0),
                "updatedBy"
        );
    }

    private AuthAccount createAuthAccount(Long id, String userIdValue, List<RoleCode> roleCodes) {
        return new AuthAccount(
                id != null && id > 0 ? new AuthAccountId(id) : null,
                new UserId(userIdValue),
                new PasswordHash("pass"),
                AccountStatus.ACTIVE
        );
    }

    // --- FindById Tests ---
    @Nested
    @DisplayName("findById")
    class FindById {

        private long testAccountId = 1L;
        private String testUserId = "testUser";

        @BeforeEach
        void setup() {
            // Data is now loaded from test-data.sql
        }

        @Test
        @DisplayName("アカウントがIDで正常に取得できること")
        void testFindById_found() {
            Optional<AuthAccount> result = authAccountRepositoryImpl.findById(new AuthAccountId(testAccountId));

            assertThat(result).isPresent();
            assertThat(result.get().id().value()).isEqualTo(testAccountId);
            assertThat(result.get().userId().value()).isEqualTo(testUserId);
            assertThat(result.get().passwordHash().value()).isEqualTo("pass");
            assertThat(result.get().accountStatus()).isEqualTo(AccountStatus.ACTIVE);
        }

        @Test
        @DisplayName("指定されたIDのアカウントが見つからないこと")
        void testFindById_notFound() {
            long nonExistentAccountId = 99L;
            Optional<AuthAccount> result = authAccountRepositoryImpl.findById(new AuthAccountId(nonExistentAccountId));
            assertThat(result).isEmpty();
        }
    }

    // --- FindByUserId Tests ---
    @Nested
    @DisplayName("findByUserId")
    class FindByUserId {

        private long testAccountId = 1L;
        private String testUserId = "testUser";

        @BeforeEach
        void setup() {
            // Data is now loaded from test-data.sql
        }

        @Test
        @DisplayName("アカウントがユーザーIDで正常に取得できること")
        void testFindByUserId_found() {
            Optional<AuthAccount> result = authAccountRepositoryImpl.findByUserId(new UserId(testUserId));

            assertThat(result).isPresent();
            assertThat(result.get().id().value()).isEqualTo(testAccountId);
            assertThat(result.get().userId().value()).isEqualTo(testUserId);
            assertThat(result.get().passwordHash().value()).isEqualTo("pass");
            assertThat(result.get().accountStatus()).isEqualTo(AccountStatus.ACTIVE);
        }

        @Test
        @DisplayName("指定されたユーザーIDのアカウントが見つからないこと")
        void testFindByUserId_notFound() {
            String nonExistentUserId = "nonExistentUser";

            Optional<AuthAccount> result = authAccountRepositoryImpl.findByUserId(new UserId(nonExistentUserId));

            assertThat(result).isEmpty();
        }
    }

    // --- Save Tests ---
    @Nested
    @DisplayName("save")
    class Save {

        private String testUserIdForInsert = "newUser";
        private List<RoleCode> testRolesForInsert = List.of(new RoleCode("ROLE_USER"));


        @BeforeEach
        void setup() {
            // Roles are now loaded from test-data.sql
        }

        @Test
        @DisplayName("新しいアカウントが正常に挿入できること")
        void testSave_insert() {
            AuthAccount newAccount = createAuthAccount(null, testUserIdForInsert, testRolesForInsert);

            authAccountRepositoryImpl.save(newAccount, Operator.system());

            // Verify insertion by finding it directly via mapper
            AuthAccountRecord insertedRecord = authAccountMapper.selectByUserId(testUserIdForInsert);
            assertThat(insertedRecord).isNotNull();
            assertThat(insertedRecord.userId()).isEqualTo(testUserIdForInsert);
            assertThat(insertedRecord.authAccountId()).isNotNull().isPositive(); // ID should be generated
            assertThat(insertedRecord.accountStatus()).isEqualTo(AccountStatus.ACTIVE.name());

        }

        @Test
        @DisplayName("既存のアカウントが正常に更新できること")
        void testSave_update() {
            long existingAccountId = 1L; // Use the account pre-loaded from test-data.sql
            AuthAccountRecord initialRecord = authAccountMapper.selectByAccountId(existingAccountId); // Get initial state

            // Create an AuthAccount representing the existing record with updated info
            AuthAccount existingAccount = new AuthAccount(
                    new AuthAccountId(initialRecord.authAccountId()),
                    new UserId("updatedUser"),
                    new PasswordHash("newEncodedPassword"),
                    AccountStatus.DISABLED
            );

            authAccountRepositoryImpl.save(existingAccount, Operator.system());

            // Verify update by finding it directly via mapper
            AuthAccountRecord updatedRecord = authAccountMapper.selectByAccountId(existingAccountId);
            assertThat(updatedRecord).isNotNull();
            assertThat(updatedRecord.userId()).isEqualTo("updatedUser");
            assertThat(updatedRecord.passwordHash()).isEqualTo("newEncodedPassword");
            assertThat(updatedRecord.accountStatus()).isEqualTo(AccountStatus.DISABLED.name());
            assertThat(updatedRecord.updatedBy()).isEqualTo(Operator.system().value());
        }
    }
}
