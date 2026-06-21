package org.example.bookshop.config.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Service
public class JwtService {

    private final JwtProperties props;
    private final SecretKey key;

    public JwtService(JwtProperties props) {
        this.props = props;
        if (props.secret() == null || props.secret().getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("app.jwt.secret must be at least 32 bytes (256 bits) for HS256");
        }
        this.key = Keys.hmacShaKeyFor(props.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String generate(String username, String role) {
        Instant now = Instant.now();
        return Jwts.builder()
            .issuer(props.issuer())
            .subject(username)
            .claim("role", role)
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(props.expirationHours(), ChronoUnit.HOURS)))
            .signWith(key)
            .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser()
            .verifyWith(key)
            .requireIssuer(props.issuer())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    public long getExpirationSeconds() {
        return props.expirationHours() * 3600;
    }
}
