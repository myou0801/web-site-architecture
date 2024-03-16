package com.myou.backend.simulator.application.service;

import com.myou.backend.simulator.domain.model.RequestData;
import com.myou.backend.simulator.domain.model.ResponseData;

public interface SimulatorService {

    ResponseData processRequest(RequestData requestData);
}
