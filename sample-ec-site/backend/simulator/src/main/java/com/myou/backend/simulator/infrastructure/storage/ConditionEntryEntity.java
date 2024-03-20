package com.myou.backend.simulator.infrastructure.storage;

import com.myou.backend.simulator.domain.model.ConditionEntry;
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
        List<ConditionEntryEntity.PolicyEntity> policies = conditionEntry
                .policies()
                .stream()
                .map(PolicyEntity::from)
                .toList();
        return new ConditionEntryEntity(conditionEntry.interfaceId(), policies);
    }

    public ConditionEntry toConditionEntry() {
        List<ConditionPolicy> policies = policies().stream().map(PolicyEntity::toConditionPolicy).toList();
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
            if (conditionRule instanceof RequestHeaderConditionRule rhr) {
                return new ConditionEntryEntity.RuleEntity(RuleType.REQUEST_HEADER, rhr.headerName(), rhr.expectedValue());
            } else if (conditionRule instanceof RequestContentConditionRule rcr) {
                return new ConditionEntryEntity.RuleEntity(RuleType.REQUEST_CONTENT, rcr.key(), rcr.expectedValue());
            } else {
                throw new RuntimeException();
            }
        }

        public ConditionRule toConditionRule() {
            return switch (type) {
                case REQUEST_HEADER -> new RequestHeaderConditionRule(key, expectedValue);
                case REQUEST_CONTENT -> new RequestContentConditionRule(key, expectedValue);
            };
        }

    }

}
