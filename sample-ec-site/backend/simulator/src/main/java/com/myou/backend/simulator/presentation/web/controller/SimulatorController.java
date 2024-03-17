package com.myou.backend.simulator.presentation.web.controller;

import com.myou.backend.simulator.application.service.SimulatorService;
import com.myou.backend.simulator.domain.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.util.MultiValueMapAdapter;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/simulator")
public class SimulatorController {

    private final SimulatorService simulatorService;

    @Autowired
    public SimulatorController(SimulatorService simulatorService) {
        this.simulatorService = simulatorService;
    }


    @PostMapping(value = "/{interfaceId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> processJsonRequest(
            @PathVariable String interfaceId,
            @RequestHeader MultiValueMap<String, String>  headers,
            @RequestBody String requestBody) {

        return doProcessRequest(interfaceId, headers, new JsonContent(requestBody));
    }


    @PostMapping(value = "/{interfaceId}", consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> processXmlRequest(
            @PathVariable String interfaceId,
            @RequestHeader MultiValueMap<String, String>  headers,
            @RequestBody String requestBody) {
        // XMLリクエストの処理とレスポンスの生成
        return doProcessRequest(interfaceId, headers, new XmlContent(requestBody));
    }

    @PostMapping(value = "/{interfaceId}", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<String> processFormRequest(
            @PathVariable String interfaceId,
            @RequestHeader MultiValueMap<String, String>  headers,
            @RequestParam MultiValueMap<String, String>  data) {
        // フォームデータの処理とレスポンスの生成
        return doProcessRequest(interfaceId, headers, new FormDataContent(data));
    }


    @GetMapping("/{interfaceId}")
    public ResponseEntity<String> processGetRequest(
            @PathVariable String interfaceId,
            @RequestHeader MultiValueMap<String, String> headers,
            @RequestParam(required = false) MultiValueMap<String, String> queryParams) {
        // GETリクエストの処理とレスポンスの生成
        return doProcessRequest(interfaceId, headers, new GetRequestContent(queryParams));
    }


    private ResponseEntity<String> doProcessRequest(String interfaceId, MultiValueMap<String, String> headers, RequestContent requestContent) {
        RequestData requestData = new RequestData(interfaceId, headers, requestContent);
        ResponseData responseData = simulatorService.processRequest(requestData);

        // ResponseEntityを使ってレスポンスヘッダ、ボディ、ステータスコードを設定
        return ResponseEntity
                .status(responseData.statusCode().value())
                .headers(new HttpHeaders(new MultiValueMapAdapter<>(responseData.responseHeaders())))
                .body(responseData.responseBody());
    }
}
