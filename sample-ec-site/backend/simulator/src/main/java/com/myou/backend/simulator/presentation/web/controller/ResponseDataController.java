package com.myou.backend.simulator.presentation.web.controller;

import com.myou.backend.simulator.application.service.ResponseDataService;
import com.myou.backend.simulator.domain.model.ResponseData;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/responses")
public class ResponseDataController {
    private final ResponseDataService responseDataService;

    public ResponseDataController(ResponseDataService responseDataService) {
        this.responseDataService = responseDataService;
    }

    @PostMapping
    public ResponseEntity<Void> createResponseData(@RequestBody ResponseDataRequest responseDataRequest) {
        responseDataService.saveResponseData(responseDataRequest.toResponseData());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{responseId}")
    public ResponseEntity<ResponseData> getResponseData(@PathVariable String responseId) {
        Optional<ResponseData> responseData = responseDataService.getResponseDataById(responseId);
        return responseData.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

}
