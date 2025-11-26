package com.myou.backend.simulator.presentation.web.controller;

import com.myou.backend.simulator.domain.model.ConditionEntry;
import com.myou.backend.simulator.domain.model.ResponseIdCondition;
import com.myou.backend.simulator.domain.policy.ConditionPolicy;
import com.myou.backend.simulator.domain.policy.rule.ConditionRule;
import com.myou.backend.simulator.domain.policy.rule.RequestContentConditionRule;
import com.myou.backend.simulator.domain.policy.rule.RequestHeaderConditionRule;
import com.myou.backend.simulator.domain.type.RuleType;

import java.util.List;

public record ConditionEntryRequest(String interfaceId, List<ResponseIdConditionRequest> responseIdConditions) {

    public ConditionEntry toConditionEntry() {
        List<ResponseIdCondition> responseIdConditionList = responseIdConditions.stream()
                .map(ResponseIdConditionRequest::toResponseIdCondition)
                .toList();
        return new ConditionEntry(interfaceId, responseIdConditionList);
    }

    public record ResponseIdConditionRequest(String responseId, PolicyRequest policy) {
        public ResponseIdCondition toResponseIdCondition() {
            return new ResponseIdCondition(responseId, policy.toConditionPolicy());
        }
    }

    public record PolicyRequest(List<RuleRequest> rules) {
        public ConditionPolicy toConditionPolicy() {
            List<ConditionRule> ruleList = rules.stream()
                    .map(RuleRequest::toConditionRule)
                    .toList();
            return new ConditionPolicy(ruleList);
        }

    }

    public record RuleRequest(RuleType type, String key, String expectedValue) {

        public ConditionRule toConditionRule() {
            return switch (type) {
                case REQUEST_HEADER -> new RequestHeaderConditionRule(key, expectedValue);
                case REQUEST_CONTENT -> new RequestContentConditionRule(key, expectedValue);
            };
        }
    }

}
