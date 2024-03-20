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
        List<ConditionEntryEntity.PolicyEntity> policies = conditionEntry.policies().stream()
                .map(p -> new ConditionEntryEntity.PolicyEntity(p.rules().stream().map(r -> {
                    if (r instanceof RequestHeaderConditionRule rhr) {
                        return new ConditionEntryEntity.RuleEntity(RuleType.REQUEST_HEADER, rhr.headerName(), rhr.expectedValue());
                    } else if (r instanceof RequestContentConditionRule rcr) {
                        return new ConditionEntryEntity.RuleEntity(RuleType.REQUEST_CONTENT, rcr.key(), rcr.expectedValue());
                    } else {
                        throw new RuntimeException();
                    }
                }).toList(), p.responseId())).toList();
        return new ConditionEntryEntity(conditionEntry.interfaceId(), policies);
    }

    public ConditionEntry toConditionEntry() {
        List<ConditionPolicy> policies = policies().stream().map(p -> {
            return new ConditionPolicy(
                    p.rules().stream()
                            .map(r -> {
                                return (ConditionRule) switch (r.type()) {
                                    case REQUEST_HEADER -> new RequestHeaderConditionRule(r.key(), r.expectedValue());
                                    case REQUEST_CONTENT -> new RequestContentConditionRule(r.key(), r.expectedValue());
                                };
                            }).toList(),
                    p.responseId());
        }).toList();
        return new ConditionEntry(interfaceId, policies);
    }


    public record PolicyEntity(List<RuleEntity> rules, String responseId) {

    }

    public record RuleEntity(RuleType type, String key, String expectedValue) {

    }

}
