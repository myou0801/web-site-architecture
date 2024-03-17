package com.myou.backend.simulator.infrastructure.storage;

import com.myou.backend.simulator.domain.model.ConditionEntry;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
@CacheConfig(cacheNames = "conditionEntries")
public class InMemoryConditionEntryRepository {

    private final ConcurrentHashMap<String, ConditionEntry> entries = new ConcurrentHashMap<>();

    @CachePut(key = "#conditionEntry.interfaceId")
    public ConditionEntry save(ConditionEntry conditionEntry) {
        entries.put(conditionEntry.interfaceId(), conditionEntry);
        return conditionEntry;
    }


    @Cacheable(key = "#interfaceId")
    public ConditionEntry findByInterfaceId(String interfaceId) {
        return entries.get(interfaceId);
    }

    public List<ConditionEntry> findAll() {
        return entries.values().stream().toList();
    }
}
