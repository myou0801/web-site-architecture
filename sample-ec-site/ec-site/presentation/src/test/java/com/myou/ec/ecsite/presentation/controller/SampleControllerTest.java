package com.myou.ec.ecsite.presentation.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@SpringBootTest //(classes = { SampleControllerTest.SampleControllerTestConfig.class })
@AutoConfigureMockMvc
// @ActiveProfiles(profiles={"docker"})
public class SampleControllerTest {

    @Autowired
    private MockMvc mvc;

    // @Test
    // void testRest() throws Exception {
    // mvc.perform(MockMvcRequestBuilders.get("/sample/rest"))
    // .andExpect(MockMvcResultMatchers.status().isOk())
    // .andExpect(MockMvcResultMatchers.content().string("success"));
    // }

    @Test
    void testRest2() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/sample/rest2"))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    // @Test
    // void testS3() throws Exception {
    // mvc.perform(MockMvcRequestBuilders.get("/sample/s3"))
    // .andExpect(MockMvcResultMatchers.status().isOk())
    // .andExpect(MockMvcResultMatchers.content().string("success"));
    // }

    // @Test
    // void testRedis() throws Exception{
    // mvc.perform(MockMvcRequestBuilders.get("/sample/redis"))
    // .andExpect(MockMvcResultMatchers.status().isOk())
    // .andExpect(MockMvcResultMatchers.content().string("success"));
    // }

    // @Configuration
    // private static class SampleControllerTestConfig {
    //     @Bean
    //     RestTemplateBuilder restTemplateBuilder(RestTemplateBuilderConfigurer restTemplateBuilderConfigurer) {
    //         RestTemplateBuilder builder = new RestTemplateBuilder();
            
    //         return restTemplateBuilderConfigurer.configure(builder);
    //     }
    // }
}
