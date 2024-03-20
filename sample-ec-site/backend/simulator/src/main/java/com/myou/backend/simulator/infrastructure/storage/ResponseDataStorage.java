package com.myou.backend.simulator.infrastructure.storage;

import org.springframework.data.keyvalue.repository.KeyValueRepository;
import org.springframework.stereotype.Component;

@Component
public interface ResponseDataStorage extends KeyValueRepository<ResponseDataEntity, String> {
}
