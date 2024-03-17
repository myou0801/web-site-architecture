package com.myou.backend.simulator.application.service;

import com.myou.backend.simulator.domain.model.ConditionEntry;

import java.util.Optional;

public interface ConditionEntryService {

    void saveConditionEntry(ConditionEntry conditionEntry);

    Optional<ConditionEntry> findByInterfaceId(String interfaceId);
}
