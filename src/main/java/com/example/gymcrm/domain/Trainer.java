package com.example.gymcrm.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "trainers")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Trainer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Getter
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(nullable = false, updatable = false)
    @Getter
    private TrainingType specialization;

    @OneToOne(optional = false, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(nullable = false, unique = true)
    private User user;

    @ManyToMany(mappedBy = "trainers")
    private Set<Trainee> trainees = new LinkedHashSet<>();

    public Trainer(User user, TrainingType specialization) {
        this.user = user;
        this.specialization = specialization;
    }

    public String getFirstName() { return user.getFirstName(); }
    public String getLastName() { return user.getLastName(); }
    public String getUsername() { return user.getUsername(); }
    public String getPassword() { return user.getPassword(); }
    public boolean isActive() { return user.isActive(); }
    public Set<Trainee> getTrainees() { return Set.copyOf(trainees); }

    public void updateProfile(String firstName, String lastName, boolean active) {
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setActive(active);
    }

    void addTrainee(Trainee trainee) {
        trainees.add(trainee);
    }

    void removeTrainee(Trainee trainee) {
        trainees.remove(trainee);
    }
}
