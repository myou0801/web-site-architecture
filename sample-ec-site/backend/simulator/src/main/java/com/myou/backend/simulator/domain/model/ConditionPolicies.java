package com.myou.backend.simulator.domain.model;

import com.myou.backend.simulator.domain.policy.ConditionPolicy;

import java.util.List;
import java.util.Optional;

public record ConditionPolicies(List<ConditionPolicy> conditionPolicies) {

    public Optional<String> findResponseId(RequestData requestData){
        return conditionPolicies.stream().filter(p -> p.apply(requestData)).map(ConditionPolicy::responseId).findFirst();
    }

}
