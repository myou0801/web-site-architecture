package com.myou.backend.simulator.infrastructure.storage;

import com.myou.backend.simulator.domain.model.ConditionEntry;
import com.myou.backend.simulator.domain.policy.ConditionPolicy;
import com.myou.backend.simulator.domain.policy.rule.ConditionRule;
import com.myou.backend.simulator.domain.policy.rule.RequestHeaderConditionRule;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class InMemoryConditionEntryRepositoryTest {
    @Autowired
    private InMemoryConditionEntryRepository inMemoryConditionEntryRepository;

    @Test
    void test_1(){


        RequestHeaderConditionRule rule1 = new RequestHeaderConditionRule("key1", "value1");
        List<ConditionRule> rules = List.of(rule1);
        List<ConditionPolicy> policies = List.of(new ConditionPolicy(rules, "response123"));

        ConditionEntry entry1 =  new ConditionEntry("interface1", policies);
        ConditionEntry entry2 =  new ConditionEntry("interface2", policies);

        inMemoryConditionEntryRepository.save(entry1);
        inMemoryConditionEntryRepository.save(entry2);

        var actual1 = inMemoryConditionEntryRepository.findByInterfaceId("interface1");
        var actual2 = inMemoryConditionEntryRepository.findByInterfaceId("interface2");

        Assertions.assertThat(actual1).isEqualTo(entry1);
        Assertions.assertThat(actual2).isEqualTo(entry2);

    }
}
