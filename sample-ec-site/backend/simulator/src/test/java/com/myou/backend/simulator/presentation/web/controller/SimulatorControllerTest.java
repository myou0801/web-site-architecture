package com.myou.backend.simulator.presentation.web.controller;

import com.myou.backend.simulator.application.service.SimulatorService;
import com.myou.backend.simulator.domain.model.HttpStatus;
import com.myou.backend.simulator.domain.model.ResponseData;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SimulatorController.class)
@AutoConfigureObservability
class SimulatorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SimulatorService simulatorService;

    @Test
    public void testProcessJsonRequest() throws Exception {

        ResponseData responseData = new ResponseData("responseId23", Map.of("Content-Type", List.of("application/json")), "{\"message\":\"Success\"}", HttpStatus.of(200));
        when(simulatorService.processRequest(any())).thenReturn(responseData);

        mockMvc.perform(post("/interfaceId123")
                        .contentType("application/json")
                        .content("{\"key\":\"value\"}"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().string(containsString("{\"message\":\"Success\"}")))
                .andDo(print());
    }

    @Test
    public void testProcessXmlRequest() throws Exception {

        ResponseData responseData = new ResponseData("responseId23",Map.of("Content-Type", List.of("application/xml")), "<response>Success</response>", HttpStatus.of(200));
        when(simulatorService.processRequest(any())).thenReturn(responseData);

        mockMvc.perform(post("/interfaceIdXml")
                        .contentType("application/xml")
                        .content("<request><key>value</key></request>"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_XML))
                .andExpect(content().string(containsString("<response>Success</response>")))
                .andDo(print());
    }

    @Test
    public void testProcessFormRequest() throws Exception {

        ResponseData responseData = new ResponseData("responseId23",Map.of("Content-Type", List.of("application/x-www-form-urlencoded")), "key=value&key2=value2", HttpStatus.of(200));
        when(simulatorService.processRequest(any())).thenReturn(responseData);

        mockMvc.perform(post("/interfaceIdForm")
                        .contentType("application/x-www-form-urlencoded")
                        .param("data", "formData"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("key=value&key2=value2")))
                .andDo(print());
    }

    @Test
    public void testProcessGetRequest() throws Exception {

        ResponseData responseData = new ResponseData("responseId23",Map.of(), "queryParamValue", HttpStatus.of(200));
        when(simulatorService.processRequest(any())).thenReturn(responseData);

        mockMvc.perform(get("/interfaceIdGet?query=testQuery"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("queryParamValue")))
                .andDo(print());
    }
}
