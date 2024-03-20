package com.myou.backend.simulator.presentation.web.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

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

        String conditionEntryJson = """
                {
                  "interfaceId": "interface123",
                  "policies": [
                    {
                      "rules": [
                        {
                          "type": "REQUEST_HEADER",
                          "key": "Content-Type",
                          "expectedValue": "application/json"
                        },
                        {
                          "type": "REQUEST_CONTENT",
                          "key": "user.name",
                          "expectedValue": "John"
                        }
                      ],
                      "responseId": "resuponseId123"
                    }
                  ]
                }""";

        mockMvc.perform(post("/api/conditions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(conditionEntryJson))
                .andExpect(status().isOk())
                .andExpect(content().string("Condition entry created successfully"))
                .andDo(print());

        String interfaceId = "interface123";
        mockMvc.perform(get("/api/conditions/{interfaceId}", interfaceId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(print());

    }


    @Test
    public void getConditionEntryByInterfaceId_ShouldReturnConditionEntry() throws Exception {
        String interfaceId = "interface123";
        mockMvc.perform(get("/api/conditions/{interfaceId}", interfaceId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andDo(print());
    }
}
