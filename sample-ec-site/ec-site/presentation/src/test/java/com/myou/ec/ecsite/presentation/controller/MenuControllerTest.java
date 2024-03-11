package com.myou.ec.ecsite.presentation.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = MenuController.class)
@AutoConfigureObservability
class MenuControllerTest {

     @Autowired
     private MockMvc mvc;

    @Test
    void testName() throws Exception {
         mvc.perform(MockMvcRequestBuilders.get("/menu/"))
         .andExpect(status().isOk())
         .andExpect(content().string("success"));
    }

}
