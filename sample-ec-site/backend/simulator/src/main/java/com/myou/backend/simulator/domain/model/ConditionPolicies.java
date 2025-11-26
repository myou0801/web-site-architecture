package com.myou.backend.simulator.domain.model;

import java.util.List;
import java.util.Optional;

public record ConditionPolicies(List<ResponseIdCondition> responseIdConditions) {

    public Optional<String> findResponseId(RequestData requestData){
        return responseIdConditions.stream()
                .filter(r -> r.matches(requestData))
                .map(ResponseIdCondition::responseId)
                .findFirst();
    }

}
