package com.myou.backend.simulator.infrastructure.repository;

import com.myou.backend.simulator.application.repository.ConditionEntryRepository;
import com.myou.backend.simulator.domain.model.ConditionEntry;
import com.myou.backend.simulator.infrastructure.storage.ConditionEntryEntity;
import com.myou.backend.simulator.infrastructure.storage.ConditionEntryStorage;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository("conditionEntryRepository")
public class ConditionEntryRepositoryImpl implements ConditionEntryRepository {
    private final ConditionEntryStorage conditionEntryStorage;

    public ConditionEntryRepositoryImpl(ConditionEntryStorage conditionEntryStorage) {
        this.conditionEntryStorage = conditionEntryStorage;
    }

    @Override
    public ConditionEntry save(ConditionEntry conditionEntry) {
        conditionEntryStorage.save(ConditionEntryEntity.from(conditionEntry));
        return conditionEntry;
    }

    @Override
    public Optional<ConditionEntry> findByInterfaceId(String interfaceId) {
        return conditionEntryStorage.findById(interfaceId)
                .flatMap(entity -> Optional.of(entity.toConditionEntry()));
    }
}
