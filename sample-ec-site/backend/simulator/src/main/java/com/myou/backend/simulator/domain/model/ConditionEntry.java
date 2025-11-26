package com.myou.backend.simulator.domain.model;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

public record ConditionEntry(String interfaceId,
                             List<ResponseIdCondition> responseIdConditions) implements Serializable {
    public Optional<String> searchResponseId(RequestData requestData) {
        return responseIdConditions.stream()
                .filter(c -> c.matches(requestData))
                .findFirst()
                .map(ResponseIdCondition::responseId);
    }
}
