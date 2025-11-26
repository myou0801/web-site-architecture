package com.myou.backend.simulator.domain.policy.rule;

import com.myou.backend.simulator.domain.model.RequestData;

public sealed interface ConditionRule permits RequestHeaderConditionRule, RequestContentConditionRule {
    boolean evaluate(RequestData requestData);
}
