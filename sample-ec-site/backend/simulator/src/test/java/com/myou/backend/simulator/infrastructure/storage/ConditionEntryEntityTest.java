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
    }


    @Test
    void toConditionEntry() {

        ConditionEntryEntity conditionEntryEntity = getConditionEntryEntity();
        ConditionEntry actual = conditionEntryEntity.toConditionEntry();

        Assertions.assertThat(actual).isEqualTo(DomainModelUtils.getConditionEntry());
    }

    @NotNull
    private static ConditionEntryEntity getConditionEntryEntity() {
        List<ConditionEntryEntity.RuleEntity> rules = List.of(
                new ConditionEntryEntity.RuleEntity(RuleType.REQUEST_HEADER, "header1", "value1"),
                new ConditionEntryEntity.RuleEntity(RuleType.REQUEST_CONTENT, "key1", "value1"));
        List<ConditionEntryEntity.ResponseIdConditionEntity> responseIdConditionEntities = List.of(
                new ConditionEntryEntity.ResponseIdConditionEntity(
                        "responseId1",
                        new ConditionEntryEntity.PolicyEntity(rules))
        );
        return new ConditionEntryEntity("interfaceId1", responseIdConditionEntities);
    }

}
