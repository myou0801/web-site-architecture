package com.myou.ec.ecsite.infrastructure.auth.repository;

import com.myou.ec.ecsite.application.auth.dto.AuthAccountDetailDto;
import com.myou.ec.ecsite.application.auth.dto.AuthAccountRoleDto;
import com.myou.ec.ecsite.application.auth.dto.AuthAccountSearchParam;
import com.myou.ec.ecsite.application.auth.dto.AuthAccountSummaryDto;
import com.myou.ec.ecsite.infrastructure.auth.mapper.AuthAccountQueryMapper;
import com.myou.ec.ecsite.infrastructure.auth.mapper.AuthAccountRoleQueryMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;


@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
class AuthAccountQueryRepositoryImplTest {

    private AuthAccountQueryRepositoryImpl authAccountQueryRepository;

    @Autowired
    private AuthAccountQueryMapper authAccountQueryMapper;

    @Autowired
    private AuthAccountRoleQueryMapper authAccountRoleQueryMapper;

    @BeforeEach
    void setUp() {
        authAccountQueryRepository = new AuthAccountQueryRepositoryImpl(authAccountQueryMapper, authAccountRoleQueryMapper);
    }

    // Assumption: The following data exists from `data.sql`.
    // Accounts:
    // 1, 'testUser', 'pass', 'ACTIVE', ...
    // 2, 'adminUser', 'pass', 'ACTIVE', ...
    // 3, 'lockedUser', 'pass', 'ACTIVE', ... (with a lock event)
    // 4, 'expiredUser', 'pass', 'ACTIVE', ... (password expired)
    // 5, 'disabledUser', 'pass', 'DISABLED', ...
    // Roles:
    // Account 1: 'ROLE_USER'
    // Account 2: 'ROLE_ADMIN', 'ROLE_USER'

    @Nested
    @DisplayName("findAccountSummaries")
    class FindAccountSummaries {

        @Test
        @DisplayName("検索条件なしですべてのアカウントサマリーを取得できること")
        void testFindAccountSummaries_noParams() {
            AuthAccountSearchParam param = new AuthAccountSearchParam();
            List<AuthAccountSummaryDto> result = authAccountQueryRepository.findAccountSummaries(param);

            // Depends on the data in `data.sql`
            assertThat(result).hasSize(5);
            assertThat(result).extracting(AuthAccountSummaryDto::getUserId)
                    .contains("testUser", "adminUser", "lockedUser", "expiredUser", "disabledUser");
        }

        @Test
        @DisplayName("ユーザーIDでアカウントサマリーを検索できること")
        void testFindAccountSummaries_byUserId() {
            AuthAccountSearchParam param = new AuthAccountSearchParam();
            param.setUserIdPrefix("testUser");
            List<AuthAccountSummaryDto> result = authAccountQueryRepository.findAccountSummaries(param);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getUserId()).isEqualTo("testUser");
        }

        @Test
        @DisplayName("ロック状態でアカウントサマリーを検索できること")
        void testFindAccountSummaries_byLockedStatus() {
            AuthAccountSearchParam param = new AuthAccountSearchParam();
            param.setLocked(true);
            List<AuthAccountSummaryDto> result = authAccountQueryRepository.findAccountSummaries(param);

            // Assumes 'lockedUser' (id=3) is the only locked user
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getUserId()).isEqualTo("lockedUser");
            assertThat(result.get(0).isLocked()).isTrue();
        }

        @Test
        @DisplayName("有効期限切れでアカウントサマリーを検索できること")
        void testFindAccountSummaries_byExpiredStatus() {
            AuthAccountSearchParam param = new AuthAccountSearchParam();
            param.setExpired(true);
            List<AuthAccountSummaryDto> result = authAccountQueryRepository.findAccountSummaries(param);
            
            // Assumes 'expiredUser' (id=4) is the only expired user
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getUserId()).isEqualTo("expiredUser");
            assertThat(result.get(0).isExpired()).isTrue();
        }

        @Test
        @DisplayName("存在しないユーザーIDで検索した場合に空のリストが返されること")
        void testFindAccountSummaries_notFound() {
            AuthAccountSearchParam param = new AuthAccountSearchParam();
            param.setUserIdPrefix("nonExistentUser");
            List<AuthAccountSummaryDto> result = authAccountQueryRepository.findAccountSummaries(param);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("countAccountSummaries")
    class CountAccountSummaries {

        @Test
        @DisplayName("検索条件なしで全件数を取得できること")
        void testCountAccountSummaries_noParams() {
            AuthAccountSearchParam param = new AuthAccountSearchParam();
            long count = authAccountQueryRepository.countAccountSummaries(param);
            assertThat(count).isEqualTo(5);
        }

        @Test
        @DisplayName("ユーザーIDで検索した件数を取得できること")
        void testCountAccountSummaries_byUserId() {
            AuthAccountSearchParam param = new AuthAccountSearchParam();
            param.setUserIdPrefix("adminUser");
            long count = authAccountQueryRepository.countAccountSummaries(param);
            assertThat(count).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("findAccountDetailByUserId")
    class FindAccountDetailByUserId {

        @Test
        @DisplayName("存在するユーザーIDでアカウント詳細を取得できること")
        void testFindAccountDetailByUserId_found() {
            String userId = "testUser";
            Optional<AuthAccountDetailDto> result = authAccountQueryRepository.findAccountDetailByUserId(userId);

            assertThat(result).isPresent();
            result.ifPresent(dto -> {
                assertAll(
                        () -> assertThat(dto.getAuthAccountId()).isEqualTo(1L),
                        () -> assertThat(dto.getUserId()).isEqualTo(userId),
                        () -> assertThat(dto.getAccountStatus()).isEqualTo("ACTIVE")
                );
            });
        }

        @Test
        @DisplayName("存在しないユーザーIDで検索した場合にOptional.emptyが返されること")
        void testFindAccountDetailByUserId_notFound() {
            String userId = "nonExistentUser";
            Optional<AuthAccountDetailDto> result = authAccountQueryRepository.findAccountDetailByUserId(userId);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findRoleRecordsByAccountIds")
    class FindRoleRecordsByAccountIds {

        @Test
        @DisplayName("複数のアカウントIDでロール情報を取得できること")
        void testFindRoleRecordsByAccountIds_multipleIds() {
            // 1='testUser' -> ROLE_USER
            // 2='adminUser' -> ROLE_ADMIN, ROLE_USER
            List<Long> accountIds = List.of(1L, 2L);
            List<AuthAccountRoleDto> result = authAccountQueryRepository.findRoleRecordsByAccountIds(accountIds);

            assertThat(result).hasSize(3);
            assertThat(result).anyMatch(dto -> dto.getAuthAccountId() == 1L && dto.getRoleCode().equals("ROLE_USER"));
            assertThat(result).anyMatch(dto -> dto.getAuthAccountId() == 2L && dto.getRoleCode().equals("ROLE_ADMIN"));
            assertThat(result).anyMatch(dto -> dto.getAuthAccountId() == 2L && dto.getRoleCode().equals("ROLE_USER"));
        }

        @Test
        @DisplayName("単一のアカウントIDでロール情報を取得できること")
        void testFindRoleRecordsByAccountIds_singleId() {
            List<Long> accountIds = List.of(1L);
            List<AuthAccountRoleDto> result = authAccountQueryRepository.findRoleRecordsByAccountIds(accountIds);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getAuthAccountId()).isEqualTo(1L);
            assertThat(result.get(0).getRoleCode()).isEqualTo("ROLE_USER");
        }

        @Test
        @DisplayName("空のIDリストを渡した場合に空のリストが返されること")
        void testFindRoleRecordsByAccountIds_emptyList() {
            List<AuthAccountRoleDto> result = authAccountQueryRepository.findRoleRecordsByAccountIds(Collections.emptyList());

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("ロールを持たないアカウントIDの場合に空のリストが返されること")
        void testFindRoleRecordsByAccountIds_noRoles() {
            // Assumes 'disabledUser' (id=5) has no roles
            List<Long> accountIds = List.of(5L);
            List<AuthAccountRoleDto> result = authAccountQueryRepository.findRoleRecordsByAccountIds(accountIds);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findRoleCodesByAccountId")
    class FindRoleCodesByAccountId {
        
        @Test
        @DisplayName("複数のロールを持つアカウントのロールコードリストを取得できること")
        void testFindRoleCodesByAccountId_multipleRoles() {
            // 2='adminUser' -> ROLE_ADMIN, ROLE_USER
            Long accountId = 2L;
            List<String> result = authAccountQueryRepository.findRoleCodesByAccountId(accountId);

            assertThat(result).hasSize(2).containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_USER");
        }

        @Test
        @DisplayName("単一のロールを持つアカウントのロールコードリストを取得できること")
        void testFindRoleCodesByAccountId_singleRole() {
            // 1='testUser' -> ROLE_USER
            Long accountId = 1L;
            List<String> result = authAccountQueryRepository.findRoleCodesByAccountId(accountId);

            assertThat(result).hasSize(1).contains("ROLE_USER");
        }

        @Test
        @DisplayName("存在しないアカウントIDの場合に空のリストが返されること")
        void testFindRoleCodesByAccountId_notFound() {
            Long accountId = 99L;
            List<String> result = authAccountQueryRepository.findRoleCodesByAccountId(accountId);
            
            assertThat(result).isEmpty();
        }
    }
}