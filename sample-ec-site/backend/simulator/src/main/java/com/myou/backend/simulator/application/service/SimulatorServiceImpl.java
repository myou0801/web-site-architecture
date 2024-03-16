package com.myou.backend.simulator.application.service;

import com.myou.backend.simulator.domain.model.HttpStatus;
import com.myou.backend.simulator.domain.model.RequestData;
import com.myou.backend.simulator.domain.model.ResponseData;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service("simulatorService")
public class SimulatorServiceImpl implements SimulatorService {

    @Override
    public ResponseData processRequest(RequestData requestData) {

        ResponseData responseData = new ResponseData(Map.of("Content-Type", List.of("application/json")), "{\"message\":\"Success\"}", HttpStatus.of(200));

        return responseData;
    }
}
