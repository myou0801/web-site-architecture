package com.myou.backend.simulator.domain.policy.rule;

import com.myou.backend.simulator.domain.model.RequestData;

import java.io.Serializable;

public record RequestContentConditionRule(String key, String expectedValue) implements ConditionRule, Serializable {
    @Override
    public boolean evaluate(RequestData requestData) {
        return requestData.content().matches(key, expectedValue);
    }
}
