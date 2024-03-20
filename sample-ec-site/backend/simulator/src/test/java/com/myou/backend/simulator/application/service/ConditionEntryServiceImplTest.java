package com.myou.backend.simulator.application.service;

import com.myou.backend.simulator.domain.model.ConditionEntry;
import com.myou.backend.simulator.domain.model.DomainModelUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ConditionEntryServiceImplTest {

    @Autowired
    private ConditionEntryServiceImpl target;
    @Test
    void saveAndFind() {
        ConditionEntry conditionEntry = DomainModelUtils.getConditionEntry();
        target.saveConditionEntry(conditionEntry);
        var actual = target.findByInterfaceId("interfaceId1");

        Assertions.assertThat(actual.isPresent()).isTrue();
        Assertions.assertThat(actual.get()).isEqualTo(conditionEntry);

    }
}
