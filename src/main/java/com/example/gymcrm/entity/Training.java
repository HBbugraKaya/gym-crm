package com.example.gymcrm.entity;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

@Entity
public class Training {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(nullable = false)
    private Trainee trainee;

    @ManyToOne(optional = false)
    @JoinColumn(nullable = false)
    private Trainer trainer;

    @Column(nullable = false)
    private String trainingName;

    @ManyToOne(optional = false)
    @JoinColumn(nullable = false)
    private TrainingType trainingType;

    @Column(nullable = false)
    private LocalDate trainingDate;

    @Column(nullable = false)
    private Integer trainingDuration;
}
