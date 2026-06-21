package org.example.bookshop.service.auth;

import org.example.bookshop.entity.User;
import org.example.bookshop.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurrentUserServiceTest {

    @Mock private UserRepository userRepository;
    @InjectMocks private CurrentUserService service;

    @Test
    void getCurrentUser_authenticatedUser_returnsUserEntity() {
        Authentication auth = Mockito.mock(Authentication.class);
        when(auth.getName()).thenReturn("ivan");
        User user = User.builder().id(1L).username("ivan").build();
        when(userRepository.findByUsername("ivan")).thenReturn(Optional.of(user));

        User result = service.getCurrentUser(auth);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getUsername()).isEqualTo("ivan");
    }

    @Test
    void getCurrentUser_unknownUsername_throws() {
        Authentication auth = Mockito.mock(Authentication.class);
        when(auth.getName()).thenReturn("ghost");
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getCurrentUser(auth))
            .isInstanceOf(UsernameNotFoundException.class)
            .hasMessageContaining("ghost");
    }
}