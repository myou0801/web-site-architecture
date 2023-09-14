package com.myou.ec.ecsite.presentation.controller;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import io.micrometer.observation.annotation.Observed;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListBucketsResponse;

@Controller
@RequestMapping("sample")
@Observed
public class SampleController {

    private static Logger logger = LoggerFactory.getLogger(SampleController.class);

    private final RestTemplate restTemplate;

    private final S3Client s3Client;

    private final RedisTemplate<String, String> redisTemplate;

    public SampleController(RestTemplateBuilder restTemplateBuilder, S3Client s3Client, RedisTemplate<String, String> redisTemplate) {
        this.restTemplate = restTemplateBuilder.build();
        this.s3Client = s3Client;
        this.redisTemplate=redisTemplate;
    }
 
    @GetMapping("rest")
    @ResponseBody
    public String rest() {

        try {
            String result = restTemplate.getForObject(new URI("http://backend:8080/sample/redis"), String.class);
            logger.info(result);
        } catch (RestClientException | URISyntaxException e) {
            logger.error("error!!", e);
            return "error";
        }

        return "success";
    }

    @GetMapping("s3")
    @ResponseBody
    public String s3() {

        String bucketName = "test-bucket";

        // s3Client.createBucket(builder -> builder.bucket(bucketName));

        ListBucketsResponse listBucketsResponse = s3Client.listBuckets();
        logger.info(listBucketsResponse.toString());

        return "success";
    }

    @GetMapping("redis")
    @ResponseBody
    public String redis() {
        redisTemplate.opsForValue().set("test", "test", Duration.ofSeconds(30));
        logger.info(redisTemplate.opsForValue().get("test"));
        return "success";
    }

}
