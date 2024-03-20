package com.myou.backend.simulator.domain.model;

import java.io.Serializable;
import java.util.Optional;

public record ConditionEntry(String interfaceId, ConditionPolicies policies) implements Serializable {
    public Optional<String> searchResponseId(RequestData requestData) {
        return policies.findResponseId(requestData);
    }
}
