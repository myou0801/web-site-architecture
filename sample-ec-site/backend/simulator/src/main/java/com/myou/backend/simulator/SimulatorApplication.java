package com.myou.backend.simulator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.map.repository.config.EnableMapRepositories;

@SpringBootApplication
@EnableCaching
@EnableMapRepositories(basePackages = "com.myou.backend.simulator.infrastructure.storage")
public class SimulatorApplication {

    public static void main(String[] args) {
        SpringApplication.run(SimulatorApplication.class, args);
    }

}
