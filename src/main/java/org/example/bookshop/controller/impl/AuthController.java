package org.example.bookshop.controller.impl;

import lombok.RequiredArgsConstructor;
import org.example.bookshop.controller.AuthApi;
import org.example.bookshop.dto.auth.AuthResponse;
import org.example.bookshop.dto.auth.LoginRequest;
import org.example.bookshop.dto.auth.RegisterRequest;
import org.example.bookshop.dto.user.UserDto;
import org.example.bookshop.service.auth.AuthService;
import org.example.bookshop.service.auth.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AuthController implements AuthApi {

    private final UserService userService;
    private final AuthService authService;

    @Override
    public ResponseEntity<UserDto> register(RegisterRequest request) {
        UserDto created = userService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Override
    public ResponseEntity<AuthResponse> login(LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
