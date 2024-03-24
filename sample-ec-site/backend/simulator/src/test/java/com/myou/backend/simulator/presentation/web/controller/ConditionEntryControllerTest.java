package com.myou.backend.simulator.presentation.web.controller;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.myou.backend.simulator.domain.type.RuleType;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc
class ConditionEntryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createConditionEntry() throws Exception {

        ConditionEntryRequest request = getConditionEntryRequest();

        mockMvc.perform(post("/api/conditions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonMapper.builder().build().writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("Condition entry created successfully"))
                .andDo(print());

        String interfaceId = "interface123";
        mockMvc.perform(get("/api/conditions/{interfaceId}", interfaceId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(print());

    }

    @NotNull
    private static ConditionEntryRequest getConditionEntryRequest() {
        ConditionEntryRequest.RuleRequest rule1 = new ConditionEntryRequest.RuleRequest(
                RuleType.REQUEST_HEADER,
                "header1",
                "value1");
        ConditionEntryRequest.RuleRequest rule2 = new ConditionEntryRequest.RuleRequest(
                RuleType.REQUEST_CONTENT,
                "/key",
                "value1");

        List<ConditionEntryRequest.ResponseIdConditionRequest> responseIdConditions = List.of(
                new ConditionEntryRequest.ResponseIdConditionRequest(
                        "responseId123",
                        new ConditionEntryRequest.PolicyRequest(List.of(rule1, rule2)))

        );
        ConditionEntryRequest reqpuest = new ConditionEntryRequest("interface123", responseIdConditions);

        String requestJson = """
                {
                  "interfaceId": "interface123",
                  "responseIdConditions": [
                    {
                      "responseId": "responseId123",
                      "policy": {
                        "rules": [
                          {
                            "type": "REQUEST_HEADER",
                            "key": "header1",
                            "expectedValue": "value1"
                          },
                          {
                            "type": "REQUEST_CONTENT",
                            "key": "/key",
                            "expectedValue": "value1"
                          }
                        ]
                      }
                    }
                  ]
                }
                """;


        return reqpuest;
    }


    @Test
    public void getConditionEntryByInterfaceId_ShouldReturnConditionEntry() throws Exception {
        String interfaceId = "interface999";
        mockMvc.perform(get("/api/conditions/{interfaceId}", interfaceId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andDo(print());
    }
}
