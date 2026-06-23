package org.example.bookshop.config.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    static final String JWT_ERROR_ATTR = "jwt_error";
    static final String JWT_ERROR_EXPIRED = "expired";
    static final String JWT_ERROR_INVALID = "invalid";

    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String header = req.getHeader(HEADER);
        if (header != null && header.startsWith(PREFIX)) {
            String token = header.substring(PREFIX.length());
            try {
                Claims claims = jwtService.parse(token);
                String username = claims.getSubject();
                String role = claims.get("role", String.class);
                var auth = new UsernamePasswordAuthenticationToken(
                    username, null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + role))
                );
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (ExpiredJwtException ex) {
                log.debug("JWT expired for request {} {}: {}", req.getMethod(), req.getRequestURI(), ex.getMessage());
                req.setAttribute(JWT_ERROR_ATTR, JWT_ERROR_EXPIRED);
                SecurityContextHolder.clearContext();
            } catch (JwtException ex) {
                log.debug("Invalid JWT for request {} {}: {}", req.getMethod(), req.getRequestURI(), ex.getMessage());
                req.setAttribute(JWT_ERROR_ATTR, JWT_ERROR_INVALID);
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(req, res);
    }
}