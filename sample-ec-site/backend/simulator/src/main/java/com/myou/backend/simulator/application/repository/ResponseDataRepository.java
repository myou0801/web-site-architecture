package com.myou.backend.simulator.application.repository;

import com.myou.backend.simulator.domain.model.ResponseData;

import java.util.Optional;

public interface ResponseDataRepository {

    ResponseData save(ResponseData responseData);

    Optional<ResponseData> findByResponseId(String responseId);

}
