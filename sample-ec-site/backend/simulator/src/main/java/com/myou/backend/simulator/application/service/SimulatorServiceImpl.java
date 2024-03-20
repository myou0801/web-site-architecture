package com.myou.backend.simulator.application.service;

import com.myou.backend.simulator.application.repository.ConditionEntryRepository;
import com.myou.backend.simulator.application.repository.ResponseDataRepository;
import com.myou.backend.simulator.domain.model.ConditionPolicies;
import com.myou.backend.simulator.domain.model.RequestData;
import com.myou.backend.simulator.domain.model.ResponseData;
import org.springframework.stereotype.Service;

@Service("simulatorService")
public class SimulatorServiceImpl implements SimulatorService {

    private final ConditionEntryRepository conditionEntryRepository;
    private final ResponseDataRepository responseDataRepository;

    public SimulatorServiceImpl(ConditionEntryRepository conditionEntryRepository, ResponseDataRepository responseDataRepository) {
        this.conditionEntryRepository = conditionEntryRepository;
        this.responseDataRepository = responseDataRepository;
    }

    @Override
    public ResponseData processRequest(RequestData requestData) {

        String responseId = conditionEntryRepository.findByInterfaceId(requestData.interfaceId())
                .flatMap(e -> new ConditionPolicies(e.policies()).findResponseId(requestData))
                .orElseThrow(() -> new IllegalArgumentException("インターフェースIDの条件エントリが存在しない、またはリクエストの内容が条件に一致しない"));

        return responseDataRepository.findByResponseId(responseId)
                .orElseThrow(() -> new IllegalArgumentException("レスポンスIDがレスポンスデータに存在しない。responseId:" + responseId));

    }
}
