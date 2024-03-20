package com.myou.backend.simulator.infrastructure.storage;

import com.myou.backend.simulator.domain.model.ConditionEntry;
import com.myou.backend.simulator.domain.model.DomainModelUtils;
import com.myou.backend.simulator.domain.type.RuleType;
import org.assertj.core.api.Assertions;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.List;

class ConditionEntryEntityTest {

    @Test
    void from() {

        ConditionEntry conditionEntry = DomainModelUtils.getConditionEntry();
        ConditionEntryEntity actual = ConditionEntryEntity.from(conditionEntry);

        Assertions.assertThat(actual).isEqualTo(getConditionEntryEntity());
        Assertions.assertThat(actual.toString())
                .isEqualTo("ConditionEntryEntity[interfaceId=interfaceId1, policies=[PolicyEntity[rules=[RuleEntity[type=REQUEST_HEADER, key=header1, expectedValue=value1], RuleEntity[type=REQUEST_CONTENT, key=key1, expectedValue=value1]], responseId=responseId1]]]");

    }


    @Test
    void toConditionEntry() {

        ConditionEntryEntity conditionEntryEntity = getConditionEntryEntity();
        ConditionEntry actual = conditionEntryEntity.toConditionEntry();

        Assertions.assertThat(actual).isEqualTo(DomainModelUtils.getConditionEntry());
        Assertions.assertThat(actual.toString())
                .isEqualTo("ConditionEntry[interfaceId=interfaceId1, policies=[ConditionPolicy[rules=[RequestHeaderConditionRule[headerName=header1, expectedValue=value1], RequestContentConditionRule[key=key1, expectedValue=value1]], responseId=responseId1]]]");
    }

    @NotNull
    private static ConditionEntryEntity getConditionEntryEntity() {
        List<ConditionEntryEntity.RuleEntity> rules = List.of(
                new ConditionEntryEntity.RuleEntity(RuleType.REQUEST_HEADER, "header1", "value1"),
                new ConditionEntryEntity.RuleEntity(RuleType.REQUEST_CONTENT, "key1", "value1"));
        List<ConditionEntryEntity.PolicyEntity> policies = List.of(new ConditionEntryEntity.PolicyEntity(rules, "responseId1"));
        return new ConditionEntryEntity("interfaceId1", policies);
    }

}