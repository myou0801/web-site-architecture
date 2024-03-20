package com.myou.backend.simulator.application.service;

import com.myou.backend.simulator.domain.model.HttpStatus;
import com.myou.backend.simulator.domain.model.ResponseData;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

@SpringBootTest
class ResponseDataServiceImplTest {

    @Autowired
    private ResponseDataServiceImpl target;

    @Test
    void saveResponseData() {

        ResponseData responseData = new ResponseData("responseId1", Map.of("header1", List.of("data1")), "success", HttpStatus.ok());
        target.saveResponseData(responseData);

        var actual = target.getResponseDataById("responseId1");

        Assertions.assertThat(actual.isPresent()).isTrue();
        Assertions.assertThat(actual.get()).isEqualTo(responseData);

    }
}
