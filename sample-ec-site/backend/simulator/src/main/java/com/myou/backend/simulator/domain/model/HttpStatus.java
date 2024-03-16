package com.myou.backend.simulator.domain.model;

public class HttpStatus {

    private final int value;

    private HttpStatus(int value) {
        if (value < 100 || value > 599) {
            throw new IllegalArgumentException("Invalid HTTP status code: " + value);
        }
        this.value = value;
    }


    public static HttpStatus ok(){
        return HttpStatus.of(200);
    }
    public static HttpStatus of(int value) {
        return new HttpStatus(value);
    }

    public int getValue() {
        return value;
    }
}
