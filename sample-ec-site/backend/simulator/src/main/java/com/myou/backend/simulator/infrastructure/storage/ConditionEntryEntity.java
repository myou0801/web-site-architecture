package com.myou.backend.simulator.infrastructure.storage;

import com.myou.backend.simulator.domain.model.ConditionEntry;
import com.myou.backend.simulator.domain.model.ConditionPolicies;
import com.myou.backend.simulator.domain.policy.ConditionPolicy;
import com.myou.backend.simulator.domain.policy.rule.ConditionRule;
import com.myou.backend.simulator.domain.policy.rule.RequestContentConditionRule;
import com.myou.backend.simulator.domain.policy.rule.RequestHeaderConditionRule;
import com.myou.backend.simulator.domain.type.RuleType;
import org.springframework.data.annotation.Id;
import org.springframework.data.keyvalue.annotation.KeySpace;

import java.util.List;

@KeySpace
public record ConditionEntryEntity(@Id String interfaceId, List<PolicyEntity> policies) {

    public static ConditionEntryEntity from(ConditionEntry conditionEntry) {
        List<PolicyEntity> policies = conditionEntry
                .policies()
                .conditionPolicies()
                .stream()
                .map(PolicyEntity::from)
                .toList();
        return new ConditionEntryEntity(conditionEntry.interfaceId(), policies);
    }

    public ConditionEntry toConditionEntry() {
        ConditionPolicies policies = new ConditionPolicies(policies().stream().map(PolicyEntity::toConditionPolicy).toList());
        return new ConditionEntry(interfaceId, policies);
    }


    public record PolicyEntity(List<RuleEntity> rules, String responseId) {

        public static PolicyEntity from(ConditionPolicy conditionPolicy) {
            return new PolicyEntity(conditionPolicy.rules().stream().map(RuleEntity::from).toList(), conditionPolicy.responseId());
        }

        public ConditionPolicy toConditionPolicy() {
            return new ConditionPolicy(
                    rules.stream().map(RuleEntity::toConditionRule).toList(),
                    responseId);
        }

    }

    public record RuleEntity(RuleType type, String key, String expectedValue) {

        public static RuleEntity from(ConditionRule conditionRule) {
            return switch (conditionRule){
                case RequestHeaderConditionRule h -> new RuleEntity(RuleType.REQUEST_HEADER, h.headerName(), h.expectedValue());
                case RequestContentConditionRule c ->  new RuleEntity(RuleType.REQUEST_CONTENT, c.key(), c.expectedValue());
                default -> throw new IllegalStateException("Unexpected value: " + conditionRule);
            };
        }

        public ConditionRule toConditionRule() {
            return switch (type) {
                case REQUEST_HEADER -> new RequestHeaderConditionRule(key, expectedValue);
                case REQUEST_CONTENT -> new RequestContentConditionRule(key, expectedValue);
            };
        }

    }

}
