package com.example.gymcrm.entity;

import java.util.HashMap;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TrainerWorkload {
    
    private String username;
    private String firstName;
    private String lastName;
    private boolean active;
    
    // Year -> (Month -> Total Minutes)
    // Example: 2026 -> { [8 (Augst) -> 180 min], [9 (Sept) -> 60 min], ... }
    private Map<Integer, Map<Integer, Integer>> years = new HashMap<>();
}
