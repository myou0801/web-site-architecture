package com.myou.backend.simulator.infrastructure.repository;

import com.myou.backend.simulator.application.repository.ResponseDataRepository;
import com.myou.backend.simulator.domain.model.ResponseData;
import com.myou.backend.simulator.infrastructure.storage.InMemoryResponseDataRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class ResponseDataRepositoryImpl implements ResponseDataRepository {

    private final InMemoryResponseDataRepository inMemoryResponseDataRepository;

    public ResponseDataRepositoryImpl(InMemoryResponseDataRepository inMemoryResponseDataRepository) {
        this.inMemoryResponseDataRepository = inMemoryResponseDataRepository;
    }

    @Override
    public ResponseData save(ResponseData responseData) {
        return inMemoryResponseDataRepository.save(responseData);
    }

    @Override
    public Optional<ResponseData> findByResponseId(String responseId) {
        return Optional.ofNullable(inMemoryResponseDataRepository.findByResponseId(responseId));
    }
}
