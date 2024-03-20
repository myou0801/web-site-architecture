package com.myou.backend.simulator.infrastructure.storage;

import com.myou.backend.simulator.domain.model.HttpStatus;
import com.myou.backend.simulator.domain.model.ResponseData;
import org.springframework.data.annotation.Id;
import org.springframework.data.keyvalue.annotation.KeySpace;

import java.util.List;
import java.util.Map;

@KeySpace
public record ResponseDataEntity(@Id String responseId, Map<String, List<String>> responseHeaders, String responseBody,
                                 int statusCode) {

    public static ResponseDataEntity from(ResponseData responseData) {
        return new ResponseDataEntity(
                responseData.responseId(),
                responseData.responseHeaders(),
                responseData.responseBody(),
                responseData.statusCode().value());
    }

    public ResponseData toResponseData() {
        return new ResponseData(
                responseId(),
                responseHeaders(),
                responseBody(),
                HttpStatus.of(statusCode));
    }
}
