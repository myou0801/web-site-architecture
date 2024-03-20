package com.myou.backend.simulator.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.map.repository.config.EnableMapRepositories;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;

@Configuration(proxyBeanMethods = false)
public class SimulatorConfig {

    @ConditionalOnProperty(prefix = "spring.data.redis.repositories", name = "enable", havingValue = "false", matchIfMissing = true)
    @EnableMapRepositories(basePackages = "com.myou.backend.simulator.infrastructure.storage")
    public static class MapRepository{}

    @ConditionalOnProperty(prefix = "spring.data.redis.repositories", name = "enable", havingValue = "true", matchIfMissing = false)
    @EnableRedisRepositories(basePackages = "com.myou.backend.simulator.infrastructure.storage")
    public static class RedisRepository{}


}
