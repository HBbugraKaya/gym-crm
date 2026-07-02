package com.example.gymcrm.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "training_types", uniqueConstraints = {
        @UniqueConstraint(name = "uk_training_types_name", columnNames = "training_type_name")
})
public class TrainingType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "training_type_name", nullable = false, updatable = false)
    private TrainingTypeName name;

    protected TrainingType() {
    }

    public TrainingType(TrainingTypeName name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public TrainingTypeName getName() {
        return name;
    }

    @Override
    public String toString() {
        return "TrainingType{id=" + id + ", name=" + name + '}';
    }
}
