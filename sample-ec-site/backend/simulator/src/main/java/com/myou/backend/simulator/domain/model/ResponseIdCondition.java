package com.myou.backend.simulator.domain.model;

import com.myou.backend.simulator.domain.policy.ConditionPolicy;

public record ResponseIdCondition(String responseId, ConditionPolicy conditionPolicy) {
    public boolean matches(RequestData requestData){
        return conditionPolicy.apply(requestData);
    }
}
