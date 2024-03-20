package com.myou.backend.simulator.application.service;

import com.myou.backend.simulator.application.repository.ConditionEntryRepository;
import com.myou.backend.simulator.domain.model.ConditionEntry;
import com.myou.backend.simulator.domain.model.DomainModelUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ConditionEntryServiceImplTest {

    @Autowired
    private ConditionEntryRepository conditionEntryRepository;

    @Test
    void saveConditionEntry() {
        ConditionEntry conditionEntry = DomainModelUtils.getConditionEntry();

        ConditionEntryServiceImpl target = new ConditionEntryServiceImpl(conditionEntryRepository);
        target.saveConditionEntry(conditionEntry);
        var actual = conditionEntryRepository.findByInterfaceId("interfaceId1");

        Assertions.assertThat(actual.isPresent()).isTrue();
        Assertions.assertThat(actual.get()).isEqualTo(conditionEntry);

    }

    @Test
    void findByInterfaceId() {
        ConditionEntry conditionEntry = DomainModelUtils.getConditionEntry();
        conditionEntryRepository.save(conditionEntry);

        ConditionEntryServiceImpl target = new ConditionEntryServiceImpl(conditionEntryRepository);
        var actual = target.findByInterfaceId("interfaceId1");

        Assertions.assertThat(actual.isPresent()).isTrue();
        Assertions.assertThat(actual.get()).isEqualTo(conditionEntry);
    }
}
