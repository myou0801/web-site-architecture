package com.myou.backend.simulator.application.service;

import com.myou.backend.simulator.application.repository.ConditionEntryRepository;
import com.myou.backend.simulator.domain.model.ConditionEntry;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service("conditionEntryService")
public class ConditionEntryServiceImpl implements ConditionEntryService {

    private final ConditionEntryRepository conditionEntryRepository;

    public ConditionEntryServiceImpl(ConditionEntryRepository conditionEntryRepository) {
        this.conditionEntryRepository = conditionEntryRepository;
    }

    @Override
    public void saveConditionEntry(ConditionEntry conditionEntry) {
        conditionEntryRepository.save(conditionEntry);
    }

    @Override
    public Optional<ConditionEntry> findByInterfaceId(String interfaceId) {
        return conditionEntryRepository.findByInterfaceId(interfaceId);
    }
}
