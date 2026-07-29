package com.example.gymcrm.security;

import com.example.gymcrm.domain.User;
import com.example.gymcrm.repository.TraineeRepository;
import com.example.gymcrm.repository.TrainerRepository;
import com.example.gymcrm.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GymUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final TraineeRepository traineeRepository;
    private final TrainerRepository trainerRepository;

    @Override
    public org.springframework.security.core.userdetails.User loadUserByUsername(String username) {
        User user = userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        List<SimpleGrantedAuthority> authorities = new ArrayList<>(2);
        if (traineeRepository.existsByUserUsernameIgnoreCase(user.getUsername())) {
            authorities.add(new SimpleGrantedAuthority("ROLE_TRAINEE"));
        }
        if (trainerRepository.existsByUserUsernameIgnoreCase(user.getUsername())) {
            authorities.add(new SimpleGrantedAuthority("ROLE_TRAINER"));
        }
        if (authorities.isEmpty()) {
            throw new UsernameNotFoundException("User has no gym profile");
        }
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(), user.getPassword(), authorities);
    }
}
