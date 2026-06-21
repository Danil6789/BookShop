package org.example.bookshop.service.auth;

import lombok.RequiredArgsConstructor;
import org.example.bookshop.config.security.JwtService;
import org.example.bookshop.dto.auth.AuthResponse;
import org.example.bookshop.dto.auth.LoginRequest;
import org.example.bookshop.entity.User;
import org.example.bookshop.mapper.UserMapper;
import org.example.bookshop.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthResponse login(LoginRequest req) {
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(req.getUsername(), req.getPassword())
        );
        User user = userRepository.findByUsername(req.getUsername())
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + req.getUsername()));
        String token = jwtService.generate(user.getUsername(), user.getRole().name());
        return userMapper.toAuthResponse(user, token, jwtService.getExpirationSeconds());
    }
}