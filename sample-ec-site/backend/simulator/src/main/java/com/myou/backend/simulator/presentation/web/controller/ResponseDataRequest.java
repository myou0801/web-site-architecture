package com.myou.backend.simulator.presentation.web.controller;

import com.myou.backend.simulator.domain.model.HttpStatus;
import com.myou.backend.simulator.domain.model.ResponseData;

import java.util.List;
import java.util.Map;

public record ResponseDataRequest(String responseId, Map<String, List<String>> responseHeaders, String responseBody, int statusCode) {

    public ResponseData toResponseData(){
        return new ResponseData(responseId, responseHeaders, responseBody, HttpStatus.of(statusCode));
    }
}
