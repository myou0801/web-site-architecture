package com.myou.backend.simulator.application.service;

import com.myou.backend.simulator.application.repository.ResponseDataRepository;
import com.myou.backend.simulator.domain.model.ResponseData;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service("responseDataService")
public class ResponseDataServiceImpl implements  ResponseDataService{

    private final ResponseDataRepository responseDataRepository;

    public ResponseDataServiceImpl(ResponseDataRepository responseDataRepository) {
        this.responseDataRepository = responseDataRepository;
    }

    @Override
    public void saveResponseData(ResponseData responseData) {
        responseDataRepository.save(responseData);
    }

    @Override
    public Optional<ResponseData> getResponseDataById(String responseId) {
        return responseDataRepository.findByResponseId(responseId);
    }
}
