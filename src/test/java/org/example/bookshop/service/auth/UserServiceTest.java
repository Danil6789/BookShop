package org.example.bookshop.service.auth;

import org.example.bookshop.dto.auth.RegisterRequest;
import org.example.bookshop.dto.user.UserDto;
import org.example.bookshop.entity.User;
import org.example.bookshop.exception.user.UsernameAlreadyTakenException;
import org.example.bookshop.mapper.UserMapper;
import org.example.bookshop.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private UserMapper userMapper;
    @Mock private PasswordEncoder passwordEncoder;
    @InjectMocks private UserService userService;

    @Test
    void register_savesUserWithEncodedPassword_whenUsernameAvailable() {
        RegisterRequest req = req("alice", "secret123", User.Role.USER);
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userMapper.toDto(any(User.class)))
            .thenReturn(new UserDto(1L, "alice", "USER"));

        UserDto result = userService.register(req);

        assertThat(result.getUsername()).isEqualTo("alice");
        assertThat(result.getRole()).isEqualTo("USER");
        verify(userRepository).save(org.mockito.ArgumentMatchers.argThat(user ->
            user.getUsername().equals("alice")
                && user.getPassword().equals("encoded-password")
                && user.getRole() == User.Role.USER
        ));
    }

    @Test
    void register_throwsUsernameAlreadyTaken_whenUsernameExists() {
        RegisterRequest req = req("dup", "secret123", User.Role.USER);
        when(userRepository.existsByUsername("dup")).thenReturn(true);

        assertThatThrownBy(() -> userService.register(req))
            .isInstanceOf(UsernameAlreadyTakenException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void getByUsername_returnsUserDto_whenUserFound() {
        User user = User.builder()
            .id(1L).username("alice").password("hash").role(User.Role.USER).build();
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(userMapper.toDto(user)).thenReturn(new UserDto(1L, "alice", "USER"));

        UserDto result = userService.getByUsername("alice");

        assertThat(result.getUsername()).isEqualTo("alice");
        assertThat(result.getRole()).isEqualTo("USER");
    }

    @Test
    void getByUsername_throwsUsernameNotFound_whenUserMissing() {
        when(userRepository.findByUsername("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getByUsername("missing"))
            .isInstanceOf(UsernameNotFoundException.class);
    }

    private static RegisterRequest req(String username, String password, User.Role role) {
        RegisterRequest r = new RegisterRequest();
        r.setUsername(username);
        r.setPassword(password);
        r.setRole(role);
        return r;
    }
}
