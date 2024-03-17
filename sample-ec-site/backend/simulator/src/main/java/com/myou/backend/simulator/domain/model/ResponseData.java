package com.myou.backend.simulator.domain.model;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public record ResponseData(String responseId, Map<String, List<String>> responseHeaders, String responseBody, HttpStatus statusCode) implements Serializable {



}
