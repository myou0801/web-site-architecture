package com.myou.backend.simulator.domain.policy.rule;

import com.myou.backend.simulator.domain.model.RequestData;

public interface ConditionRule {
    boolean evaluate(RequestData requestData);
}
