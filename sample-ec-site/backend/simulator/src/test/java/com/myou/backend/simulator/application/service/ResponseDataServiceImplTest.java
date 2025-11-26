package com.myou.backend.simulator.application.service;

import com.myou.backend.simulator.application.repository.ResponseDataRepository;
import com.myou.backend.simulator.domain.model.HttpStatus;
import com.myou.backend.simulator.domain.model.ResponseData;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@SpringBootTest
class ResponseDataServiceImplTest {

    @Autowired
    private ResponseDataRepository responseDataRepository;

    @Test
    void saveResponseData() {

        ResponseData responseData = new ResponseData("responseId1", Map.of("header1", List.of("data1")), "success", HttpStatus.ok());
        ResponseDataServiceImpl target = new ResponseDataServiceImpl(responseDataRepository);
        target.saveResponseData(responseData);

        Optional<ResponseData> actual = responseDataRepository.findByResponseId("responseId1");

        Assertions.assertThat(actual.isPresent()).isTrue();
        Assertions.assertThat(actual.get()).isEqualTo(responseData);

    }

    @Test
    void getResponseDataById() {

        ResponseData responseData = new ResponseData(
                "responseId1",
                Map.of("header1",
                        List.of("data1")),
                "success",
                HttpStatus.ok());
        responseDataRepository.save(responseData);

        ResponseDataServiceImpl target = new ResponseDataServiceImpl(responseDataRepository);
        Optional<ResponseData> actual = target.getResponseDataById("responseId1");

        Assertions.assertThat(actual.isPresent()).isTrue();
        Assertions.assertThat(actual.get()).isEqualTo(responseData);
    }

    @Test
    void getAllResponseData() {
        ResponseData responseData1 = new ResponseData(
                "responseId1",
                Map.of("header1", List.of("data1")),
                "success",
                HttpStatus.ok());
        ResponseData responseData2 = new ResponseData(
                "responseId2",
                null,
                "success",
                HttpStatus.ok());
        List<ResponseData> responseDataList = List.of(responseData1, responseData2);
        responseDataRepository.saveAll(responseDataList);

        ResponseDataServiceImpl target = new ResponseDataServiceImpl(responseDataRepository);
        List<ResponseData> actual = target.getAllResponseData();

        Assertions.assertThat(actual).containsAll(responseDataList);
    }
}
