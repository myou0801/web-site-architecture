package com.myou.backend.simulator.domain.policy.rule;

import com.myou.backend.simulator.domain.model.RequestData;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public record RequestHeaderConditionRule(String headerName, String expectedValue) implements ConditionRule, Serializable {
    @Override
    public boolean evaluate(RequestData requestData) {
        Map<String, List<String>> headers = requestData.requestHeaders();
        return headers.containsKey(headerName) && headers.get(headerName).contains(expectedValue);
    }
}
