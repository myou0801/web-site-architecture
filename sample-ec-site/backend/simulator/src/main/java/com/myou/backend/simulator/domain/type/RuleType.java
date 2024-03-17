package com.myou.backend.simulator.domain.type;

import com.myou.backend.simulator.domain.model.RequestContent;
import com.myou.backend.simulator.domain.policy.rule.ConditionRule;
import com.myou.backend.simulator.domain.policy.rule.RequestContentConditionRule;
import com.myou.backend.simulator.domain.policy.rule.RequestHeaderConditionRule;

public enum RuleType {

    REQUEST_HEADER(RequestHeaderConditionRule.class),
    REQUEST_CONTENT(RequestContentConditionRule.class);

    private final Class<? extends ConditionRule> cls;
    RuleType(Class<? extends ConditionRule> cls) {
        this.cls = cls;
    }

    public Class<? extends ConditionRule> getRuleClass(){
        return this.cls;
    }

}
