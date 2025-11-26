package com.myou.backend.simulator.application.repository;

import com.myou.backend.simulator.domain.model.ResponseData;

import java.util.List;
import java.util.Optional;

public interface ResponseDataRepository {

    ResponseData save(ResponseData responseData);

    void saveAll(List<ResponseData> responseDataList);

    Optional<ResponseData> findByResponseId(String responseId);

    List<ResponseData> findAll();

}
