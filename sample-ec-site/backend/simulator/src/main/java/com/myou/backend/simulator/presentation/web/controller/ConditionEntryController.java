package com.myou.backend.simulator.presentation.web.controller;

import com.myou.backend.simulator.application.service.ConditionEntryService;
import com.myou.backend.simulator.domain.model.ConditionEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/conditions")
public class ConditionEntryController {

    private final ConditionEntryService conditionEntryService;

    @Autowired
    public ConditionEntryController(ConditionEntryService conditionEntryService) {
        this.conditionEntryService = conditionEntryService;
    }

    @PostMapping
    public ResponseEntity<String> createConditionEntry(@RequestBody ConditionEntryRequest conditionEntryRequest) {
        conditionEntryService.saveConditionEntry(conditionEntryRequest.toConditionEntry());
        return ResponseEntity.ok("Condition entry created successfully");
    }

    @GetMapping("/{interfaceId}")
    public ResponseEntity<ConditionEntry> getConditionEntryByInterfaceId(@PathVariable("interfaceId") String interfaceId) {

        // TODO レスポンスデータ用のレスポンスデータを作成する必要がある
        Optional<ConditionEntry> entry = conditionEntryService.findByInterfaceId(interfaceId);
        return entry.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }
}
