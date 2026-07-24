package com.example.gymcrm.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "training_types")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TrainingType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "training_type_name", nullable = false, updatable = false, unique = true)
    private TrainingTypeName name;

    public TrainingType(TrainingTypeName name) {
        this.name = name;
    }
}
