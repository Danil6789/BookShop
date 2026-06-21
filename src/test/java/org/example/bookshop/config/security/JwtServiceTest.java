package org.example.bookshop.config.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String VALID_SECRET =
        "test-secret-must-be-at-least-32-bytes-long-string-required-here";
    private static final long EXPIRATION_HOURS = 1L;
    private static final String ISSUER = "bookshop-test";

    private static JwtService newService() {
        return new JwtService(new JwtProperties(VALID_SECRET, EXPIRATION_HOURS, ISSUER));
    }

    @Test
    void generate_returnsJwtStringWithThreeSegments() {
        String token = newService().generate("alice", "USER");
        assertThat(token).isNotBlank();
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    void parse_extractsUsernameAndRole() {
        JwtService service = newService();
        String token = service.generate("bob", "ADMIN");
        Claims claims = service.parse(token);
        assertThat(claims.getSubject()).isEqualTo("bob");
        assertThat(claims.get("role", String.class)).isEqualTo("ADMIN");
    }

    @Test
    void parse_throwsOnMalformedToken() {
        assertThatThrownBy(() -> newService().parse("not-a-jwt"))
            .isInstanceOf(JwtException.class);
    }

    @Test
    void getExpirationSeconds_returnsExpectedValue() {
        assertThat(newService().getExpirationSeconds())
            .isEqualTo(EXPIRATION_HOURS * 3600);
    }

    @Test
    void constructor_throwsOnShortSecret() {
        assertThatThrownBy(() -> new JwtService(
            new JwtProperties("too-short", EXPIRATION_HOURS, ISSUER)
        )).isInstanceOf(IllegalStateException.class);
    }
}
