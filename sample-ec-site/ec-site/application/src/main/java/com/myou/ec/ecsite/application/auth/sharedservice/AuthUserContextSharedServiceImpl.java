package com.myou.ec.ecsite.application.auth.sharedservice;

import com.myou.ec.ecsite.domain.auth.exception.AuthDomainException;
import com.myou.ec.ecsite.domain.auth.model.AuthUser;
import com.myou.ec.ecsite.domain.auth.model.LoginHistory;
import com.myou.ec.ecsite.domain.auth.model.value.LoginId;
import com.myou.ec.ecsite.domain.auth.model.value.RoleCode;
import com.myou.ec.ecsite.domain.auth.repository.AuthLoginHistoryRepository;
import com.myou.ec.ecsite.domain.auth.repository.AuthUserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AuthUserContextSharedServiceImpl implements AuthUserContextSharedService {

    private final AuthUserRepository authUserRepository;
    private final AuthLoginHistoryRepository loginHistoryRepository;

    public AuthUserContextSharedServiceImpl(AuthUserRepository authUserRepository,
                                            AuthLoginHistoryRepository loginHistoryRepository) {
        this.authUserRepository = authUserRepository;
        this.loginHistoryRepository = loginHistoryRepository;
    }

    @Override
    public Optional<AuthUser> findCurrentUser() {
        String loginId = getCurrentLoginIdFromSecurityContext();
        if (loginId == null) {
            return Optional.empty();
        }
        return authUserRepository.findByLoginId(new LoginId(loginId));
    }

    @Override
    public AuthUser getCurrentUserOrThrow() {
        return findCurrentUser()
                .orElseThrow(() -> new AuthDomainException("ログインユーザ情報が取得できません。"));
    }

    @Override
    public Optional<LocalDateTime> findPreviousLoginAt() {
        return findCurrentUser()
                .map(AuthUser::id)
                .flatMap(loginHistoryRepository::findPreviousSuccessLoginAt)
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

    private String getCurrentLoginIdFromSecurityContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        // シンプルに username=loginId として扱う方針
        if (principal instanceof org.springframework.security.core.userdetails.User userDetails) {
            return userDetails.getUsername();
        }
        if (principal instanceof String s) {
            return s;
        }
        return null;
    }
}
