package com.myou.backend.simulator.domain.model;

import com.myou.backend.simulator.domain.policy.ConditionPolicy;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

public record ConditionEntry(String interfaceId, List<ConditionPolicy> policies) implements Serializable {
    public Optional<String> searchResonseId(RequestData requestData) {
        return policies.stream().filter(p -> p.apply(requestData)).findFirst().map(ConditionPolicy::responseId);
    }
}
