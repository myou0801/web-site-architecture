package com.myou.ec.backend.presentation.controller;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import software.amazon.awssdk.services.s3.S3Client;

@RestController
@RequestMapping("sample")
public class SampleController {
    
    private static Logger logger = LoggerFactory.getLogger(SampleController.class);


    private final RedisTemplate<String, String> redisTemplate;

    public SampleController(RestTemplateBuilder restTemplateBuilder, S3Client s3Client, RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate=redisTemplate;
    }

    @GetMapping("redis")
    @ResponseBody
    public String redis() {
        redisTemplate.opsForValue().set("test2", "test", Duration.ofSeconds(30));
        logger.info(redisTemplate.opsForValue().get("test"));
        return "success";
    }
}
