package com.example.gymcrm.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "trainees")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Trainee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Getter
    private Long id;

    @Getter
    private LocalDate dateOfBirth;

    @Getter
    private String address;

    @OneToOne(optional = false, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(nullable = false, unique = true)
    private User user;

    @ManyToMany
    @JoinTable(name = "trainee_trainer",
            joinColumns = @JoinColumn(name = "trainee_id"),
            inverseJoinColumns = @JoinColumn(name = "trainer_id"))
    private Set<Trainer> trainers = new LinkedHashSet<>();

    @OneToMany(mappedBy = "trainee", cascade = CascadeType.REMOVE)
    private List<Training> trainings = new ArrayList<>();

    public Trainee(User user, LocalDate dateOfBirth, String address) {
        this.user = user;
        this.dateOfBirth = dateOfBirth;
        this.address = address;
    }

    public String getFirstName() { return user.getFirstName(); }
    public String getLastName() { return user.getLastName(); }
    public String getUsername() { return user.getUsername(); }
    public String getPassword() { return user.getPassword(); }
    public boolean isActive() { return user.isActive(); }
    public Set<Trainer> getTrainers() { return Set.copyOf(trainers); }

    public void updateProfile(String firstName, String lastName, LocalDate dateOfBirth, String address, boolean active) {
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setActive(active);
        this.dateOfBirth = dateOfBirth;
        this.address = address;
    }

    public void assignTrainer(Trainer trainer) {
        if (trainers.add(trainer)) {
            trainer.addTrainee(this);
        }
    }

    public void replaceTrainers(Collection<Trainer> newTrainers) {
        clearTrainers();
        newTrainers.forEach(this::assignTrainer);
    }

    public void clearTrainers() {
        trainers.forEach(trainer -> trainer.removeTrainee(this));
        trainers.clear();
    }
}
