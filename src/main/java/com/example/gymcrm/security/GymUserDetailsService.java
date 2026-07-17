package com.example.gymcrm.security;

import com.example.gymcrm.domain.User;
import com.example.gymcrm.repository.TraineeRepository;
import com.example.gymcrm.repository.TrainerRepository;
import com.example.gymcrm.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class GymUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final TraineeRepository traineeRepository;
    private final TrainerRepository trainerRepository;

    public GymUserDetailsService(UserRepository userRepository,
                                 TraineeRepository traineeRepository,
                                 TrainerRepository trainerRepository) {
        this.userRepository = userRepository;
        this.traineeRepository = traineeRepository;
        this.trainerRepository = trainerRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        List<SimpleGrantedAuthority> authorities = new ArrayList<>(2);
        if (traineeRepository.findByUsername(user.getUsername()).isPresent()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_TRAINEE"));
        }
        if (trainerRepository.findByUsername(user.getUsername()).isPresent()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_TRAINER"));
        }
        if (authorities.isEmpty()) {
            throw new UsernameNotFoundException("User has no gym profile");
        }
        return new GymUserPrincipal(user, authorities);
    }
}
