package com.myou.backend.simulator.domain.model;

import org.springframework.lang.Nullable;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public record ResponseData(String responseId,
                           @Nullable
                           Map<String, List<String>> responseHeaders,
                           String responseBody,
                           HttpStatus statusCode) implements Serializable {



}
