package com.myou.backend.simulator.domain.model;

import java.util.List;
import java.util.Map;

public record FormDataContent(Map<String, List<String>> formData) implements RequestContent {
}
