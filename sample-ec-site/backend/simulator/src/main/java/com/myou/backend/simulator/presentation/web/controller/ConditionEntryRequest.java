package com.myou.backend.simulator.presentation.web.controller;

import com.myou.backend.simulator.domain.model.ConditionEntry;
import com.myou.backend.simulator.domain.policy.ConditionPolicy;
import com.myou.backend.simulator.domain.policy.rule.ConditionRule;
import com.myou.backend.simulator.domain.policy.rule.RequestContentConditionRule;
import com.myou.backend.simulator.domain.policy.rule.RequestHeaderConditionRule;
import com.myou.backend.simulator.domain.type.RuleType;

import java.util.List;

public record ConditionEntryRequest(String interfaceId, List<PolicyRequest> policies) {

    public ConditionEntry toConditionEntry() {
        List<ConditionPolicy> policyList = policies.stream().map(PolicyRequest::toConditionPolicy).toList();
        return new ConditionEntry(interfaceId, policyList);
    }

    public record PolicyRequest(List<RuleDefinitionRequest> rules, String responseId) {
        public ConditionPolicy toConditionPolicy() {
            List<ConditionRule> ruleList = rules.stream().map(RuleDefinitionRequest::toConditionRule).toList();
            return new ConditionPolicy(ruleList, responseId);
        }

    }

    public record RuleDefinitionRequest(RuleType type, String key, String expectedValue) {

        public ConditionRule toConditionRule() {
            return switch (type) {
                case REQUEST_HEADER -> (ConditionRule) new RequestHeaderConditionRule(key, expectedValue);
                case REQUEST_CONTENT -> (ConditionRule) new RequestContentConditionRule(key, expectedValue);
            };
        }
    }

}
