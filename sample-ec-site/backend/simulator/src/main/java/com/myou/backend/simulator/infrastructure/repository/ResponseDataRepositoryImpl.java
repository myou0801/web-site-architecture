package com.myou.backend.simulator.infrastructure.repository;

import com.myou.backend.simulator.application.repository.ResponseDataRepository;
import com.myou.backend.simulator.domain.model.ResponseData;
import com.myou.backend.simulator.infrastructure.storage.ResponseDataEntity;
import com.myou.backend.simulator.infrastructure.storage.ResponseDataStorage;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class ResponseDataRepositoryImpl implements ResponseDataRepository {

    private final ResponseDataStorage responseDataStorage;

    public ResponseDataRepositoryImpl(ResponseDataStorage responseDataStorage) {
        this.responseDataStorage = responseDataStorage;
    }

    @Override
    public ResponseData save(ResponseData responseData) {
        ResponseDataEntity entity = responseDataStorage.save(ResponseDataEntity.from(responseData));
        return entity.toResponseData();
    }

    @Override
    public void saveAll(List<ResponseData> responseDataList) {
        responseDataStorage.saveAll(responseDataList.stream().map(ResponseDataEntity::from).toList());
    }

    @Override
    public Optional<ResponseData> findByResponseId(String responseId) {
        return responseDataStorage.findById(responseId)
                .flatMap(e -> Optional.of(e.toResponseData()));
    }

    @Override
    public List<ResponseData> findAll() {
        return responseDataStorage.findAll()
                .stream()
                .map(ResponseDataEntity::toResponseData)
                .toList();
    }
}
