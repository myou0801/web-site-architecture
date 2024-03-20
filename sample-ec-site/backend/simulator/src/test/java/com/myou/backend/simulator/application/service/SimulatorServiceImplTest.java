package com.myou.backend.simulator.application.service;

import com.myou.backend.simulator.application.repository.ConditionEntryRepository;
import com.myou.backend.simulator.application.repository.ResponseDataRepository;
import com.myou.backend.simulator.domain.model.*;
import com.myou.backend.simulator.domain.policy.ConditionPolicy;
import com.myou.backend.simulator.domain.policy.rule.ConditionRule;
import com.myou.backend.simulator.domain.policy.rule.RequestContentConditionRule;
import com.myou.backend.simulator.domain.policy.rule.RequestHeaderConditionRule;
import org.assertj.core.api.Assertions;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

@SpringBootTest
class SimulatorServiceImplTest {

    @Autowired
    private ConditionEntryRepository conditionEntryRepository;

    @Autowired
    private ResponseDataRepository responseDataRepository;

    @Test
    void processRequest_success() {

        conditionEntryRepository.save(getConditionEntry());
        ResponseData responseData = new ResponseData(
                "responseId1",
                Map.of("header1", List.of("data1")),
                "success",
                HttpStatus.ok());
        responseDataRepository.save(responseData);

        SimulatorServiceImpl target = new SimulatorServiceImpl(conditionEntryRepository, responseDataRepository);

        RequestData requestData = new RequestData("interfaceId1",
                Map.of("header1", List.of("value1")),
                new JsonContent("""
                        {"key1": "value1"}"""));
        ResponseData actual = target.processRequest(requestData);

        Assertions.assertThat(actual.responseBody()).isEqualTo("success");
        Assertions.assertThat(actual.statusCode()).isEqualTo(HttpStatus.ok());

    }

    @Test
    void processRequest_failure1() {

        conditionEntryRepository.save(getConditionEntry());
        ResponseData responseData = new ResponseData(
                "responseId1",
                Map.of("header1", List.of("data1")),
                "success",
                HttpStatus.ok());
        responseDataRepository.save(responseData);

        SimulatorServiceImpl target = new SimulatorServiceImpl(conditionEntryRepository, responseDataRepository);

        RequestData requestData = new RequestData("fail1",
                Map.of("header1", List.of("value1")),
                new JsonContent("""
                        {"key1": "value1"}"""));
        Assertions.assertThatThrownBy(() -> target.processRequest(requestData))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("インターフェースIDの条件エントリが存在しない");

    }

    @Test
    void processRequest_failure2() {

        conditionEntryRepository.save(getConditionEntry());
        ResponseData responseData = new ResponseData(
                "responseId1",
                Map.of("header1", List.of("data1")),
                "success",
                HttpStatus.ok());
        responseDataRepository.save(responseData);

        SimulatorServiceImpl target = new SimulatorServiceImpl(conditionEntryRepository, responseDataRepository);

        RequestData requestData = new RequestData("interfaceId1",
                Map.of("header1", List.of("value1")),
                new JsonContent("""
                        {"fail": "value1"}"""));
        Assertions.assertThatThrownBy(() -> target.processRequest(requestData))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("またはリクエストの内容が条件に一致しない");

    }

    @Test
    void processRequest_failure3() {

        conditionEntryRepository.save(getConditionEntry2());

        SimulatorServiceImpl target = new SimulatorServiceImpl(conditionEntryRepository, responseDataRepository);

        RequestData requestData = new RequestData("interfaceId2",
                Map.of("header2", List.of("value2")),
                new JsonContent("""
                        {"key2": "value2"}"""));
        Assertions.assertThatThrownBy(() -> target.processRequest(requestData))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("レスポンスIDがレスポンスデータに存在しない");

    }


    @NotNull
    public static ConditionEntry getConditionEntry() {
        List<ConditionRule> rules = List.of(
                new RequestHeaderConditionRule("header1", "value1"),
                new RequestContentConditionRule("/key1", "value1"));
        ConditionPolicy conditionPolicy = new ConditionPolicy(rules, "responseId1");
        List<ConditionPolicy> policies = List.of(conditionPolicy);
        return new ConditionEntry("interfaceId1", policies);
    }

    @NotNull
    public static ConditionEntry getConditionEntry2() {
        List<ConditionRule> rules = List.of(
                new RequestHeaderConditionRule("header2", "value2"),
                new RequestContentConditionRule("/key2", "value2"));
        ConditionPolicy conditionPolicy = new ConditionPolicy(rules, "responseId2");
        List<ConditionPolicy> policies = List.of(conditionPolicy);
        return new ConditionEntry("interfaceId2", policies);
    }
}
