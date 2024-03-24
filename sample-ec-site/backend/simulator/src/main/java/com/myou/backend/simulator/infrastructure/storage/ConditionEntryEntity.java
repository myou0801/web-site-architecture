package com.myou.backend.simulator.infrastructure.storage;

import com.myou.backend.simulator.domain.model.ConditionEntry;
import com.myou.backend.simulator.domain.model.ResponseIdCondition;
import com.myou.backend.simulator.domain.policy.ConditionPolicy;
import com.myou.backend.simulator.domain.policy.rule.ConditionRule;
import com.myou.backend.simulator.domain.policy.rule.RequestContentConditionRule;
import com.myou.backend.simulator.domain.policy.rule.RequestHeaderConditionRule;
import com.myou.backend.simulator.domain.type.RuleType;
import org.springframework.data.annotation.Id;
import org.springframework.data.keyvalue.annotation.KeySpace;

import java.util.List;

@KeySpace
public record ConditionEntryEntity(@Id String interfaceId, List<ResponseIdConditionEntity> responseIdConditions) {

    public static ConditionEntryEntity from(ConditionEntry conditionEntry) {
        List<ResponseIdConditionEntity> responseIdConditionEntityList = conditionEntry
                .responseIdConditions()
                .stream()
                .map(ResponseIdConditionEntity::from)
                .toList();
        return new ConditionEntryEntity(conditionEntry.interfaceId(), responseIdConditionEntityList);
    }

    public ConditionEntry toConditionEntry() {
        List<ResponseIdCondition> conditionPolicies = responseIdConditions.stream()
                .map(ResponseIdConditionEntity::toResponseIdCondition)
                .toList();
        return new ConditionEntry(interfaceId, conditionPolicies);
    }

    public record ResponseIdConditionEntity(String responseId, PolicyEntity policy) {
        public static ResponseIdConditionEntity from(ResponseIdCondition responseIdCondition) {
            return new ResponseIdConditionEntity(
                    responseIdCondition.responseId(),
                    PolicyEntity.from(responseIdCondition.conditionPolicy()));
        }

        public ResponseIdCondition toResponseIdCondition() {
            return new ResponseIdCondition(
                    responseId,
                    policy.toConditionPolicy());
        }
    }

    public record PolicyEntity(List<RuleEntity> rules) {

        public static PolicyEntity from(ConditionPolicy conditionPolicy) {
            return new PolicyEntity(
                    conditionPolicy.rules()
                            .stream()
                            .map(RuleEntity::from)
                            .toList());
        }

        public ConditionPolicy toConditionPolicy() {
            return new ConditionPolicy(
                    rules.stream().map(RuleEntity::toConditionRule).toList());
        }

    }

    public record RuleEntity(RuleType type, String key, String expectedValue) {

        public static RuleEntity from(ConditionRule conditionRule) {
            return switch (conditionRule) {
                case RequestHeaderConditionRule(String headerName, String expectedValue) ->
                        new RuleEntity(RuleType.REQUEST_HEADER, headerName, expectedValue);
                case RequestContentConditionRule(String key, String expectedValue) ->
                        new RuleEntity(RuleType.REQUEST_CONTENT, key, expectedValue);
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
