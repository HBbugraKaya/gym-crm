package com.example.gymcrm.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "trainers")
public class Trainer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "specialization_id", nullable = false)
    private TrainingType specialization;

    @OneToOne(optional = false, cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @ManyToMany(mappedBy = "trainers")
    private Set<Trainee> trainees = new LinkedHashSet<>();

    @OneToMany(mappedBy = "trainer")
    private List<Training> trainings = new ArrayList<>();

    protected Trainer() {
    }

    public Trainer(User user, TrainingType specialization) {
        this.user = user;
        this.specialization = specialization;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return user.getId();
    }

    public String getFirstName() {
        return user.getFirstName();
    }

    public String getLastName() {
        return user.getLastName();
    }

    public String getUsername() {
        return user.getUsername();
    }

    public String getPassword() {
        return user.getPassword();
    }

    public boolean isActive() {
        return user.isActive();
    }

    public TrainingType getSpecialization() {
        return specialization;
    }

    public Set<Trainee> getTrainees() {
        return Collections.unmodifiableSet(trainees);
    }

    public List<Training> getTrainings() {
        return Collections.unmodifiableList(trainings);
    }

    public void updateProfile(String firstName, String lastName, TrainingType specialization, boolean active) {
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setActive(active);
        this.specialization = specialization;
    }

    public void changePassword(String password) {
        user.changePassword(password);
    }

    public void setActive(boolean active) {
        user.setActive(active);
    }

    Set<Trainee> getTraineesInternal() {
        return trainees;
    }

    @Override
    public String toString() {
        return "Trainer{id=" + id + ", username='" + getUsername() + "', active=" + isActive() + '}';
    }
}
