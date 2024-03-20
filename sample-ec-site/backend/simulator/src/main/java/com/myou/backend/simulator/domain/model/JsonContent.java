package com.myou.backend.simulator.domain.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

public record JsonContent(String content, JsonNode rootNode) implements RequestContent {

    private static final Logger logger = LoggerFactory.getLogger(JsonContent.class);

    public JsonContent(String content) {
        this(content, parseJson(content));
    }

    private static JsonNode parseJson(String json) {
        Assert.hasText(json, () -> "json content is null or blank");
        try {
            return JsonMapper.builder().build().readTree(json);
        } catch (Exception e) {
            throw new IllegalArgumentException("Provided string is not valid JSON.", e);
        }
    }

    @Override
    public String toString() {
        return "JsonContent{" +
                "content='" + content + '\'' +
                '}';
    }

    @Override
    public boolean matches(String key, String expectedValue) {
        try {
            JsonNode targetNode = rootNode.at(key);
            return !targetNode.isMissingNode() && targetNode.asText().equals(expectedValue);
        } catch (Exception e) {
            logger.error("JSONデータの解析に失敗", e);
            return false;
        }
    }
}
