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
            List<Long> ids = rows.stream().map(AuthAccountSummaryDto::getAuthAccountId).collect(Collectors.toList());
            rolesById = authAccountQueryRepository.findRoleRecordsByAccountIds(ids).stream()
                    .collect(Collectors.groupingBy(
                            AuthAccountRoleDto::getAuthAccountId,
                            Collectors.mapping(AuthAccountRoleDto::getRoleCode, Collectors.toCollection(LinkedHashSet::new))
                    ));
        }

        List<AuthAccountSummaryDto> items = rows;

        PageDto<AuthAccountSummaryDto> page = new PageDto<>();
        page.setItems(items);
        page.setTotalCount(total);
        page.setPageNumber(request.getPage().getPageNumber());
        page.setPageSize(request.getPage().getPageSize());
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
        List<String> roles = authAccountQueryRepository.findRoleCodesByAccountId(r.getAuthAccountId());

        r.setRoleCodes(new LinkedHashSet<>(roles));
        return Optional.of(r);
    }

    private void validate(AuthAccountSearchRequest request) {
        if (request == null) throw new IllegalArgumentException("request is required");
        if (request.getPage() == null) throw new IllegalArgumentException("page is required");
        if (request.getPage().getPageNumber() < 0) throw new IllegalArgumentException("pageNumber must be >= 0");
        if (request.getPage().getPageSize() <= 0 || request.getPage().getPageSize() > 100)
            throw new IllegalArgumentException("pageSize must be 1..100");

        if (request.getSort() != null) {
            Set<String> allowedKeys = Set.of("USER_ID", "CREATED_AT", "UPDATED_AT", "LAST_LOGIN_AT");
            if (request.getSort().getSortKey() != null && !allowedKeys.contains(request.getSort().getSortKey())) {
                throw new IllegalArgumentException("invalid sortKey: " + request.getSort().getSortKey());
            }
            if (request.getSort().getDirection() != null &&
                    !("ASC".equals(request.getSort().getDirection()) || "DESC".equals(request.getSort().getDirection()))) {
                throw new IllegalArgumentException("invalid sort direction: " + request.getSort().getDirection());
            }
        }
    }

    private AuthAccountSearchParam toParam(AuthAccountSearchRequest req) {
        AuthAccountSearchParam p = new AuthAccountSearchParam();
        p.setUserIdPrefix(req.getUserIdPrefix());
        p.setAccountStatuses(req.getAccountStatuses());
        p.setLocked(req.getLocked());
        p.setExpired(req.getExpired());

        p.setSortKey((req.getSort() == null || req.getSort().getSortKey() == null) ? "USER_ID" : req.getSort().getSortKey());
        p.setSortDirection((req.getSort() == null || req.getSort().getDirection() == null) ? "ASC" : req.getSort().getDirection());

        p.setLimit(req.getPage().getPageSize());
        p.setOffset(req.getPage().getPageNumber() * req.getPage().getPageSize());
        return p;
    }
}
