package com.example.gymcrm.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "trainings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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

    @Column(name = "training_name", nullable = false)
    private String name;

    @ManyToOne(optional = false)
    @JoinColumn(nullable = false)
    private TrainingType trainingType;

    @Column(name = "training_date", nullable = false)
    private LocalDate date;

    @Column(name = "training_duration", nullable = false)
    private int durationMinutes;

    public Training(Trainee trainee, Trainer trainer, String name, TrainingType trainingType,
                    LocalDate date, int durationMinutes) {
        this.trainee = trainee;
        this.trainer = trainer;
        this.name = name;
        this.trainingType = trainingType;
        this.date = date;
        this.durationMinutes = durationMinutes;
    }
}
