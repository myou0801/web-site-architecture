package com.myou.backend.simulator.infrastructure.storage;

import com.myou.backend.simulator.domain.model.ConditionEntry;
import com.myou.backend.simulator.domain.model.ResponseData;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Component
@CacheConfig(cacheNames = "requestData")
public class InMemoryResponseDataRepository {

    private final ConcurrentHashMap<String, ResponseData> map = new ConcurrentHashMap<>();

    @CachePut(key = "#responseData.responseId")
    public ResponseData save(ResponseData responseData) {
        map.put(responseData.responseId(), responseData);
        return responseData;
    }

    @Cacheable(key = "#responseId")
    public ResponseData findByResponseId(String responseId) {
        return map.get(responseId);
    }

    public List<ResponseData> findAll() {
        return map.values().stream().toList();
    }

}
