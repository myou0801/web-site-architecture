package com.myou.backend.simulator.infrastructure.repository;

import com.myou.backend.simulator.application.repository.ConditionEntryRepository;
import com.myou.backend.simulator.domain.model.ConditionEntry;
import com.myou.backend.simulator.infrastructure.storage.InMemoryConditionEntryRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class ConditionEntryRepositoryImpl implements ConditionEntryRepository {

    private final InMemoryConditionEntryRepository inMemoryConditionEntryRepository;

    public ConditionEntryRepositoryImpl(InMemoryConditionEntryRepository inMemoryConditionEntryRepository) {
        this.inMemoryConditionEntryRepository = inMemoryConditionEntryRepository;
    }

    @Override
    public ConditionEntry save(ConditionEntry conditionEntry) {
        return inMemoryConditionEntryRepository.save(conditionEntry);
    }

    @Override
    public Optional<ConditionEntry> findByInterfaceId(String interfaceId) {
        return Optional.ofNullable(inMemoryConditionEntryRepository.findByInterfaceId(interfaceId));
    }
}
