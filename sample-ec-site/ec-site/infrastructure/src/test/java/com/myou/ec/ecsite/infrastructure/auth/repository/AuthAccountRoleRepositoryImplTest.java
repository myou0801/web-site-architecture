package com.myou.ec.ecsite.infrastructure.auth.repository;

import com.myou.ec.ecsite.domain.auth.model.value.AuthAccountId;
import com.myou.ec.ecsite.domain.auth.model.value.Operator;
import com.myou.ec.ecsite.domain.auth.model.value.RoleCode;
import com.myou.ec.ecsite.infrastructure.auth.mapper.AuthAccountRoleMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
class AuthAccountRoleRepositoryImplTest {

    private AuthAccountRoleRepositoryImpl authAccountRoleRepository;

    @Autowired
    private AuthAccountRoleMapper authAccountRoleMapper;

    @BeforeEach
    void setUp() {
        authAccountRoleRepository = new AuthAccountRoleRepositoryImpl(authAccountRoleMapper, Clock.systemDefaultZone());
    }

    @Nested
    @DisplayName("findRolesByAccountId")
    class FindRolesByAccountId {

        @Test
        @DisplayName("複数のロールを持つアカウントのロールを取得できること")
        void testFindRoles_multipleRoles() {
            // from data.sql, adminUser(id=2) has ROLE_ADMIN and ROLE_USER
            AuthAccountId accountId = new AuthAccountId(2L);
            Set<RoleCode> roles = authAccountRoleRepository.findRolesByAccountId(accountId);

            assertThat(roles).hasSize(2)
                    .extracting(RoleCode::value)
                    .containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_USER");
        }

        @Test
        @DisplayName("単一のロールを持つアカウントのロールを取得できること")
        void testFindRoles_singleRole() {
            // from data.sql, testUser(id=1) has ROLE_USER
            AuthAccountId accountId = new AuthAccountId(1L);
            Set<RoleCode> roles = authAccountRoleRepository.findRolesByAccountId(accountId);

            assertThat(roles).hasSize(1)
                    .extracting(RoleCode::value)
                    .contains("ROLE_USER");
        }

        @Test
        @DisplayName("ロールを持たないアカウントの場合に空のSetが返されること")
        void testFindRoles_noRoles() {
            // from data.sql, disabledUser(id=5) has no roles
            AuthAccountId accountId = new AuthAccountId(5L);
            Set<RoleCode> roles = authAccountRoleRepository.findRolesByAccountId(accountId);

            assertThat(roles).isEmpty();
        }
    }

    @Nested
    @DisplayName("addRole/removeRole")
    class AddAndRemoveRole {

        @Test
        @DisplayName("アカウントに新しいロールを追加および削除できること")
        void testAddAndRemoveRole() {
            AuthAccountId accountId = new AuthAccountId(1L); // testUser
            RoleCode newRole = new RoleCode("ROLE_TEMP");
            Operator operator = Operator.of("test-operator");

            // --- Add Role ---
            int addResult = authAccountRoleRepository.addRole(accountId, newRole, operator);
            assertThat(addResult).isEqualTo(1);

            // Verify addition
            List<String> rolesAfterAdd = authAccountRoleMapper.selectRoleCodesByAccountId(accountId.value());
            assertThat(rolesAfterAdd).contains("ROLE_USER", "ROLE_TEMP");

            // --- Remove Role ---
            int removeResult = authAccountRoleRepository.removeRole(accountId, newRole);
            assertThat(removeResult).isEqualTo(1);

            // Verify removal
            List<String> rolesAfterRemove = authAccountRoleMapper.selectRoleCodesByAccountId(accountId.value());
            assertThat(rolesAfterRemove).doesNotContain("ROLE_TEMP").contains("ROLE_USER");
        }

        @Test
        @DisplayName("存在しないロールを削除しようとすると0が返ること")
        void testRemoveNonExistentRole() {
            AuthAccountId accountId = new AuthAccountId(1L);
            RoleCode nonExistentRole = new RoleCode("ROLE_NON_EXISTENT");

            int removeResult = authAccountRoleRepository.removeRole(accountId, nonExistentRole);
            assertThat(removeResult).isEqualTo(0);
        }
    }
}
