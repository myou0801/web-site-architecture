package com.myou.backend.simulator.presentation.web.controller;

import com.myou.backend.simulator.domain.policy.rule.RequestHeaderConditionRule;
import com.myou.backend.simulator.domain.type.RuleType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ConditionEntryRequestTest {

    @Test
    void toConditionEntry() {


        ConditionEntryRequest.RuleDefinitionRequest ruleDefinitionRequest = new ConditionEntryRequest.RuleDefinitionRequest(RuleType.REQUEST_HEADER, "key1", "value1");
        List<ConditionEntryRequest.RuleDefinitionRequest> rules = new ArrayList<>();
        rules.add(ruleDefinitionRequest);

        ConditionEntryRequest.PolicyRequest policy = new ConditionEntryRequest.PolicyRequest(rules, "responseId");
        List<ConditionEntryRequest.PolicyRequest> policies = new ArrayList<>();
        policies.add(policy);
        ConditionEntryRequest request = new ConditionEntryRequest("interfaceId", policies);

        var actual = request.toConditionEntry();

        System.out.print(actual);

    }
}
