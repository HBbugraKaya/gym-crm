package com.example.gymcrm.workload;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import static org.assertj.core.api.Assertions.assertThat;

class TrainerWorkloadApplicationTest {

    @Test
    void isSpringBootApplication() {
        assertThat(TrainerWorkloadApplication.class.getAnnotation(SpringBootApplication.class)).isNotNull();
    }
}
