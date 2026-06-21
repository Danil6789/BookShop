package org.example.bookshop.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private long expiresIn;
    private String username;
    private String role;
}
