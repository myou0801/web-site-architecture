package com.myou.backend.simulator.domain.model;

import com.myou.backend.simulator.domain.policy.ConditionPolicy;
import com.myou.backend.simulator.domain.policy.rule.ConditionRule;
import com.myou.backend.simulator.domain.policy.rule.RequestContentConditionRule;
import com.myou.backend.simulator.domain.policy.rule.RequestHeaderConditionRule;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class DomainModelUtils {

    @NotNull
    public static ConditionEntry getConditionEntry() {
        List<ConditionRule> rules = List.of(
                new RequestHeaderConditionRule("header1", "value1"),
                new RequestContentConditionRule("key1", "value1"));
        ConditionPolicy conditionPolicy = new ConditionPolicy(rules);
        List<ResponseIdCondition> responseIdConditions = List.of(new ResponseIdCondition("responseId1", conditionPolicy));
        return new ConditionEntry("interfaceId1", responseIdConditions);
    }


}
