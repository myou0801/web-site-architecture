package com.myou.backend.simulator.domain.model;

import java.util.List;
import java.util.Map;

public record ResponseData(Map<String, List<String>> responseHeaders, String responseBody, HttpStatus statusCode) {
}
