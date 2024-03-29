package com.myou.backend.simulator.presentation.web.controller;

import com.myou.backend.simulator.application.service.ResponseDataService;
import com.myou.backend.simulator.domain.model.ResponseData;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/responses")
public class ResponseDataController {
    private final ResponseDataService responseDataService;

    public ResponseDataController(ResponseDataService responseDataService) {
        this.responseDataService = responseDataService;
    }

    @PostMapping("/{responseId}")
    public ResponseEntity<Void> createResponseData(@PathVariable("responseId") String responseId,
                                                   @RequestBody ResponseDataRequest responseDataRequest) {
        responseDataService.saveResponseData(responseDataRequest.toResponseData());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{responseId}")
    public ResponseEntity<ResponseData> getResponseData(@PathVariable("responseId") String responseId) {

        // TODO レスポンスデータ用のレスポンスデータを作成する必要がある
        Optional<ResponseData> responseData = responseDataService.getResponseDataById(responseId);
        return responseData.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/")
    public ResponseEntity<List<ResponseData>> getAllResponseData() {

        // TODO レスポンスデータ用のレスポンスデータを作成する必要がある
        List<ResponseData> responseData = responseDataService.getAllResponseData();
        return ResponseEntity.ok(responseData);
    }



}
