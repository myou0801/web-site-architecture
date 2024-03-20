package com.myou.backend.simulator.domain.model;

import org.springframework.util.Assert;

import java.io.Serializable;

public record HttpStatus(int value) implements Serializable {

    public HttpStatus {
        Assert.isTrue(value >= 100 && value <= 999,
                () -> "Status code '" + value + "' should be a three-digit positive integer");
    }

    public static HttpStatus ok(){
        return HttpStatus.of(200);
    }
    public static HttpStatus of(int value) {
        return new HttpStatus(value);
    }

}
