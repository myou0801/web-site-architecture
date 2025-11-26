package com.myou.backend.simulator.presentation.web.controller;

import com.myou.backend.simulator.domain.model.DomainModelUtils;
import com.myou.backend.simulator.domain.type.RuleType;
import org.assertj.core.api.Assertions;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.List;

class ConditionEntryRequestTest {

    @Test
    void toConditionEntry() {
        ConditionEntryRequest request = getConditionEntryRequest();
        var actual = request.toConditionEntry();
        Assertions.assertThat(actual).isEqualTo(DomainModelUtils.getConditionEntry());
    }

    @NotNull
    private static ConditionEntryRequest getConditionEntryRequest() {
        ConditionEntryRequest.RuleRequest ruleDefinitionRequest1 =
                new ConditionEntryRequest.RuleRequest(
                        RuleType.REQUEST_HEADER,
                        "header1",
                        "value1"
                );
        ConditionEntryRequest.RuleRequest ruleDefinitionRequest2 =
                new ConditionEntryRequest.RuleRequest(
                        RuleType.REQUEST_CONTENT,
                        "key1",
                        "value1");
        List<ConditionEntryRequest.RuleRequest> rules = List.of(ruleDefinitionRequest1, ruleDefinitionRequest2);

        ConditionEntryRequest.PolicyRequest policy = new ConditionEntryRequest.PolicyRequest(rules);
        ConditionEntryRequest.ResponseIdConditionRequest responseIdConditionRequest = new ConditionEntryRequest.ResponseIdConditionRequest(
                "responseId1",
                policy);
        return new ConditionEntryRequest(
                "interfaceId1",
                List.of(responseIdConditionRequest));
    }


}
