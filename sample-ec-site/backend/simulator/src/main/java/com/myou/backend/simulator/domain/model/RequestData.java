package com.myou.backend.simulator.domain.model;

import java.util.List;
import java.util.Map;

public record RequestData(String interfaceId,
                          Map<String, List<String>> requestHeaders,
                          RequestContent content) {
}
