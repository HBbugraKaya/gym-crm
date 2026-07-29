package com.example.gymcrm.web.dto;

import java.time.LocalDate;

public record TraineeProfileResponse(String username, String firstName, String lastName, LocalDate dateOfBirth, String address) {

}
