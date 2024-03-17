package com.myou.backend.simulator.infrastructure.storage;

import com.myou.backend.simulator.domain.model.ConditionEntry;
import com.myou.backend.simulator.domain.policy.ConditionPolicy;
import com.myou.backend.simulator.domain.type.RuleType;
import com.myou.backend.simulator.presentation.web.controller.ConditionEntryRequest;

import java.io.Serializable;
import java.util.List;

public record ConditionEntryEntity(String interfaceId, List<PolicyEntity> policies) implements Serializable {

//    public static ConditionEntryEntity from(ConditionEntry conditionEntry){
//        ConditionEntryEntity entity = new ConditionEntryEntity(conditionEntry.interfaceId(), conditionEntry.policies().stream().map(p -> new ConditionPolicy(p.rules().stream().map(r -> {
//
//        }).toList(),p.responseId())));
//    }

    public record PolicyEntity(List<RuleEntity> rules, String responseId) implements Serializable {

    }

    public record RuleEntity(RuleType type, String key, String expectedValue) implements Serializable {

    }

}
