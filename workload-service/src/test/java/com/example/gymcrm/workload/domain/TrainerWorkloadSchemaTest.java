package com.example.gymcrm.workload.domain;

import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import static org.assertj.core.api.Assertions.assertThat;

class TrainerWorkloadSchemaTest {
    @Test
    void documentUsesTheExpectedCollectionAndTrainerNameIndex() {
        Document document = TrainerWorkload.class.getAnnotation(Document.class);
        CompoundIndex nameIndex = TrainerWorkload.class.getAnnotation(CompoundIndex.class);

        assertThat(document.collection()).isEqualTo("trainer_workloads");
        assertThat(nameIndex.name()).isEqualTo("trainer_name_idx");
        assertThat(nameIndex.def())
                .isEqualTo("{'trainerFirstName': 1, 'trainerLastName': 1}");
    }

    @Test
    void usernameAndStatusFieldsHaveTheExpectedPersistenceMapping() throws NoSuchFieldException {
        Indexed usernameIndex = TrainerWorkload.class
                .getDeclaredField("trainerUsername")
                .getAnnotation(Indexed.class);
        Field activeField = TrainerWorkload.class
                .getDeclaredField("active")
                .getAnnotation(Field.class);

        assertThat(usernameIndex.unique()).isTrue();
        assertThat(usernameIndex.name()).isEqualTo("trainer_username_idx");
        assertThat(activeField.value()).isEqualTo("isActive");
    }
}
