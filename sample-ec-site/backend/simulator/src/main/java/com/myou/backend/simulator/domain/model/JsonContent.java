package com.myou.backend.simulator.domain.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.apache.catalina.mapper.Mapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record JsonContent(String content) implements RequestContent{

    private static final  Logger logger = LoggerFactory.getLogger(JsonContent.class);

    private static final JsonMapper mapper = JsonMapper.builder().build();

    @Override
    public boolean matches(String key, String expectedValue) {
        try {
            JsonNode rootNode = mapper.readTree(content);
            JsonNode targetNode = rootNode.at(key);
            return !targetNode.isMissingNode() && targetNode.asText().equals(expectedValue);
        } catch (Exception e) {
            logger.error("JSONデータの解析に失敗", e);
            return false;
        }
    }
}
