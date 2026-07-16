package com.example.gymcrm.generator;

import com.example.gymcrm.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UniqueUsernameGeneratorTest {
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UniqueUsernameGenerator generator;

    @Test
    void generatesBaseUsernameWhenNameIsUnique() {
        when(userRepository.countByFirstNameAndLastName("John", "Smith")).thenReturn(0L);
        when(userRepository.existsByUsername("John.Smith")).thenReturn(false);

        assertThat(generator.generate(" John ", " Smith ")).isEqualTo("John.Smith");

        verify(userRepository).countByFirstNameAndLastName("John", "Smith");
        verify(userRepository).existsByUsername("John.Smith");
    }

    @Test
    void appendsNextAvailableNumericSuffixForDuplicateNames() {
        when(userRepository.countByFirstNameAndLastName("John", "Smith")).thenReturn(2L);
        when(userRepository.existsByUsername("John.Smith2")).thenReturn(true);
        when(userRepository.existsByUsername("John.Smith3")).thenReturn(false);

        assertThat(generator.generate("John", "Smith")).isEqualTo("John.Smith3");

        verify(userRepository).existsByUsername("John.Smith2");
        verify(userRepository).existsByUsername("John.Smith3");
    }
}
