package com.example.gymcrm.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "trainees")
public class Trainee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "address")
    private String address;

    @OneToOne(optional = false, cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @ManyToMany
    @JoinTable(name = "trainee_trainer",
            joinColumns = @JoinColumn(name = "trainee_id"),
            inverseJoinColumns = @JoinColumn(name = "trainer_id"))
    private Set<Trainer> trainers = new LinkedHashSet<>();

    @OneToMany(mappedBy = "trainee", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<Training> trainings = new ArrayList<>();

    protected Trainee() {
    }

    public Trainee(User user, LocalDate dateOfBirth, String address) {
        this.user = user;
        this.dateOfBirth = dateOfBirth;
        this.address = address;
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

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public String getAddress() {
        return address;
    }

    public Set<Trainer> getTrainers() {
        return Collections.unmodifiableSet(trainers);
    }

    public List<Training> getTrainings() {
        return Collections.unmodifiableList(trainings);
    }

    public void updateProfile(String firstName, String lastName, LocalDate dateOfBirth, String address, boolean active) {
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setActive(active);
        this.dateOfBirth = dateOfBirth;
        this.address = address;
    }

    public void assignTrainer(Trainer trainer) {
        if (trainers.add(trainer)) {
            trainer.getTraineesInternal().add(this);
        }
    }

    public void replaceTrainers(Collection<Trainer> newTrainers) {
        clearTrainers();
        newTrainers.forEach(this::assignTrainer);
    }

    public void clearTrainers() {
        for (Trainer trainer : new ArrayList<>(trainers)) {
            trainer.getTraineesInternal().remove(this);
        }
        trainers.clear();
    }

    Set<Trainer> getTrainersInternal() {
        return trainers;
    }

    @Override
    public String toString() {
        return "Trainee{id=" + id + ", username='" + getUsername() + "', active=" + isActive() + '}';
    }
}
