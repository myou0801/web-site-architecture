package com.myou.backend.simulator.infrastructure.storage;

import org.springframework.data.keyvalue.repository.KeyValueRepository;
import org.springframework.stereotype.Component;

@Component
public interface ConditionEntryStorage extends KeyValueRepository<ConditionEntryEntity, String> {
}
