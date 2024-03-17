package com.myou.backend.simulator.presentation.web.controller;

import com.myou.backend.simulator.domain.model.ConditionEntry;
import com.myou.backend.simulator.domain.policy.ConditionPolicy;
import com.myou.backend.simulator.domain.policy.rule.ConditionRule;
import com.myou.backend.simulator.domain.policy.rule.RequestContentConditionRule;
import com.myou.backend.simulator.domain.policy.rule.RequestHeaderConditionRule;
import com.myou.backend.simulator.domain.type.RuleType;

import java.util.ArrayList;
import java.util.List;

public record ConditionEntryRequest(String interfaceId, List<PolicyRequest> policies) {

    public ConditionEntry toConditionEntry() {
        List<ConditionPolicy> policyList = policies.stream().map(p ->
                {
                    List<ConditionRule> rules = p.rules.stream().map(r ->
                            switch (r.type) {
                                case REQUEST_HEADER ->
                                        (ConditionRule) new RequestHeaderConditionRule(r.key(), r.expectedValue());
                                case REQUEST_CONTENT ->
                                        (ConditionRule) new RequestContentConditionRule(r.key(), r.expectedValue());
                            }
                    ).toList();
                    return new ConditionPolicy(rules, p.responseId());
                }
        ).toList();

        return new ConditionEntry(interfaceId, policyList);
    }

    public record PolicyRequest(List<RuleDefinitionRequest> rules, String responseId) {
    }

    public record RuleDefinitionRequest(RuleType type, String key, String expectedValue) {
    }

}
