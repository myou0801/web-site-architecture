package com.myou.ec.ecsite.application.auth.sharedservice;

import com.myou.ec.ecsite.application.auth.dto.*;
import com.myou.ec.ecsite.application.auth.repository.AuthAccountQueryRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AuthAccountQuerySharedServiceImpl implements AuthAccountQuerySharedService {

    private final AuthAccountQueryRepository authAccountQueryRepository;

    public AuthAccountQuerySharedServiceImpl(AuthAccountQueryRepository authAccountQueryRepository) {
        this.authAccountQueryRepository = authAccountQueryRepository;
    }

    @Override
    public PageDto<AuthAccountSummaryDto> search(AuthAccountSearchRequest request) {
        validate(request);

        AuthAccountSearchParam param = toParam(request);

        List<AuthAccountSummaryDto> rows = authAccountQueryRepository.findAccountSummaries(param);
        long total = authAccountQueryRepository.countAccountSummaries(param);

        Map<Long, Set<String>> rolesById = Collections.emptyMap();
        if (!rows.isEmpty()) {
            List<Long> ids = rows.stream().map(r -> r.authAccountId).collect(Collectors.toList());
            rolesById = authAccountQueryRepository.findRoleRecordsByAccountIds(ids).stream()
                    .collect(Collectors.groupingBy(
                            r -> r.authAccountId,
                            Collectors.mapping(r -> r.roleCode, Collectors.toCollection(LinkedHashSet::new))
                    ));
        }

        List<AuthAccountSummaryDto> items = rows;

        PageDto<AuthAccountSummaryDto> page = new PageDto<>();
        page.items = items;
        page.totalCount = total;
        page.pageNumber = request.page.pageNumber;
        page.pageSize = request.page.pageSize;
        return page;
    }

    @Override
    public Optional<AuthAccountDetailDto> findByUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId is required");
        }

        Optional<AuthAccountDetailDto> recOpt = authAccountQueryRepository.findAccountDetailByUserId(userId);
        if (recOpt.isEmpty()) return Optional.empty();

        AuthAccountDetailDto r = recOpt.get();
        List<String> roles = authAccountQueryRepository.findRoleCodesByAccountId(r.authAccountId);

        r.roleCodes = new LinkedHashSet<>(roles);
        return Optional.of(r);
    }

    private void validate(AuthAccountSearchRequest request) {
        if (request == null) throw new IllegalArgumentException("request is required");
        if (request.page == null) throw new IllegalArgumentException("page is required");
        if (request.page.pageNumber < 0) throw new IllegalArgumentException("pageNumber must be >= 0");
        if (request.page.pageSize <= 0 || request.page.pageSize > 100)
            throw new IllegalArgumentException("pageSize must be 1..100");

        if (request.sort != null) {
            Set<String> allowedKeys = Set.of("USER_ID","CREATED_AT","UPDATED_AT","LAST_LOGIN_AT");
            if (request.sort.sortKey != null && !allowedKeys.contains(request.sort.sortKey)) {
                throw new IllegalArgumentException("invalid sortKey: " + request.sort.sortKey);
            }
            if (request.sort.direction != null &&
                    !("ASC".equals(request.sort.direction) || "DESC".equals(request.sort.direction))) {
                throw new IllegalArgumentException("invalid sort direction: " + request.sort.direction);
            }
        }
    }

    private AuthAccountSearchParam toParam(AuthAccountSearchRequest req) {
        AuthAccountSearchParam p = new AuthAccountSearchParam();
        p.userIdPrefix = req.userIdPrefix;
        p.accountStatuses = req.accountStatuses;
        p.locked = req.locked;
        p.expired = req.expired;

        p.sortKey = (req.sort == null || req.sort.sortKey == null) ? "USER_ID" : req.sort.sortKey;
        p.sortDirection = (req.sort == null || req.sort.direction == null) ? "ASC" : req.sort.direction;

        p.limit = req.page.pageSize;
        p.offset = req.page.pageNumber * req.page.pageSize;
        return p;
    }
}
