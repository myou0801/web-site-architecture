package com.myou.backend.simulator.domain.model;

public interface RequestContent {

    boolean matches(String key, String expectedValue);

}
