package com.myou.backend.simulator.domain.model;

import java.io.Serializable;

public record HttpStatus(int value) implements Serializable {

    public HttpStatus {
        if (value < 100 || value > 599) {
            throw new IllegalArgumentException("Invalid HTTP status code: " + value);
        }
    }

    public static HttpStatus ok(){
        return HttpStatus.of(200);
    }
    public static HttpStatus of(int value) {
        return new HttpStatus(value);
    }

}
