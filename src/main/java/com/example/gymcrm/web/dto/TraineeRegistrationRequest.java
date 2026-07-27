package com.example.gymcrm.web.dto;

import java.time.LocalDate;

public record TraineeRegistrationRequest(String firstName, String lastName, LocalDate dateOfBirth, String address) {

}
