package com.myou.backend.simulator.domain.model;

import java.util.List;
import java.util.Map;

public record GetRequestContent(Map<String, List<String>> queryParam) implements RequestContent {
}
