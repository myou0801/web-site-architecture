package com.myou.ec.ecsite.application.auth.sharedservice;

import com.myou.ec.ecsite.domain.auth.exception.AuthDomainException;
import com.myou.ec.ecsite.domain.auth.model.AuthAccount;
import com.myou.ec.ecsite.domain.auth.model.LoginHistory;
import com.myou.ec.ecsite.domain.auth.model.value.RoleCode;
import com.myou.ec.ecsite.domain.auth.model.value.UserId;
import com.myou.ec.ecsite.domain.auth.repository.AuthAccountRepository;
import com.myou.ec.ecsite.domain.auth.repository.AuthLoginHistoryRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AuthAccountContextSharedServiceImpl implements AuthAccountContextSharedService {

    private final AuthAccountRepository authAccountRepository;
    private final AuthLoginHistoryRepository loginHistoryRepository;

    public AuthAccountContextSharedServiceImpl(AuthAccountRepository authAccountRepository,
                                            AuthLoginHistoryRepository loginHistoryRepository) {
        this.authAccountRepository = authAccountRepository;
        this.loginHistoryRepository = loginHistoryRepository;
    }

    @Override
    public Optional<AuthAccount> findCurrentUser() {
        String userId = getCurrentUserIdFromSecurityContext();
        if (userId == null) {
            return Optional.empty();
        }
        return authAccountRepository.findByUserId(new UserId(userId));
    }

    @Override
    public AuthAccount getCurrentUserOrThrow() {
        return findCurrentUser()
                .orElseThrow(() -> new AuthDomainException("ログインアカウント情報が取得できません。"));
    }

    @Override
    public Optional<LocalDateTime> findPreviousLoginAt() {
        return findCurrentUser()
                .map(AuthAccount::id)
                .flatMap(loginHistoryRepository::findLatestSuccessByAccountId)
                .map(LoginHistory::loginAt);
    }

    @Override
    public List<RoleCode> getCurrentUserRoles() {
        return getCurrentUserOrThrow().roleCodes();
    }

    @Override
    public boolean hasRole(RoleCode roleCode) {
        return getCurrentUserOrThrow()
                .roleCodes()
                .stream()
                .anyMatch(rc -> rc.value().equals(roleCode.value()));
    }

    private String getCurrentUserIdFromSecurityContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        // シンプルに username=userId として扱う方針
        if (principal instanceof org.springframework.security.core.userdetails.User userDetails) {
            return userDetails.getUsername();
        }
        if (principal instanceof String s) {
            return s;
        }
        return null;
    }
}
