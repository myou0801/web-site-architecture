package com.myou.backend.simulator.application.repository;

import com.myou.backend.simulator.domain.model.ConditionEntry;

import java.util.List;
import java.util.Optional;

public interface ConditionEntryRepository {

    ConditionEntry save(ConditionEntry conditionEntry);

    Optional<ConditionEntry> findByInterfaceId(String interfaceId);

    List<ConditionEntry> findAll();

}
