package org.example.bookshop.service.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.bookshop.dto.auth.RegisterRequest;
import org.example.bookshop.dto.user.UserDto;
import org.example.bookshop.entity.User;
import org.example.bookshop.exception.user.UsernameAlreadyTakenException;
import org.example.bookshop.mapper.UserMapper;
import org.example.bookshop.repository.UserRepository;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserDto register(RegisterRequest req) {
        if (userRepository.existsByUsername(req.getUsername())) {
            throw new UsernameAlreadyTakenException(req.getUsername());
        }
        User user = User.builder()
            .username(req.getUsername())
            .password(passwordEncoder.encode(req.getPassword()))
            .role(req.getRole())
            .build();
        User saved = userRepository.save(user);
        log.info("User registered: {}", saved.getUsername());
        return userMapper.toDto(saved);
    }

    @Transactional(readOnly = true)
    public UserDto getByUsername(String username) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        return userMapper.toDto(user);
    }
}