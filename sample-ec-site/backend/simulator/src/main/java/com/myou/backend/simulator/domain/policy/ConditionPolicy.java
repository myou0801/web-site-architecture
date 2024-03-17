package com.myou.backend.simulator.domain.policy;

import com.myou.backend.simulator.domain.model.RequestData;
import com.myou.backend.simulator.domain.policy.rule.ConditionRule;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public record ConditionPolicy(List<ConditionRule> rules, String responseId)  implements Serializable {

    public boolean apply(RequestData requestData) {
        return rules.stream().allMatch(rule -> rule.evaluate(requestData));
    }


}
