package com.myou.backend.simulator.application.service;

import com.myou.backend.simulator.domain.model.ResponseData;

import java.util.List;
import java.util.Optional;

public interface ResponseDataService {

    void saveResponseData(ResponseData responseData);

    Optional<ResponseData> getResponseDataById(String responseId);

    List<ResponseData> getAllResponseData();

}
