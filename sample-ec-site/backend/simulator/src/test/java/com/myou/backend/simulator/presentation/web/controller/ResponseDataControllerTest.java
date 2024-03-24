package com.myou.backend.simulator.presentation.web.controller;

import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ResponseDataControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createResponseData() throws Exception {

        String responseId = "response123";
        ResponseDataRequest responseData = new ResponseDataRequest(
                responseId,
                Map.of("Content-Type", List.of("application/json")),
                "{\"key\":\"value\"}",
                200
        );

        mockMvc.perform(post("/api/responses/{responseId}", responseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonMapper.builder().build().writeValueAsString(responseData)))
                .andExpect(status().isOk())
                .andDo(print());


        mockMvc.perform(get("/api/responses/{responseId}", responseId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(print());

    }

    @Test
    void getResponseData() throws Exception {
        String responseId = "response123";
        mockMvc.perform(get("/api/responses/{responseId}", responseId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andDo(print());
    }
}
