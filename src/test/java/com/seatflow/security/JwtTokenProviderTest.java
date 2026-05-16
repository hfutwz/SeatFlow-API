package com.seatflow.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * JwtTokenProvider 单元测试 — 基于 PRD A-AUTH01/A-AUTH02
 *
 * <p>验证 JWT 的生成、解析、过期检测等行为
 */
class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private final String secret = "test-secret-key-must-be-at-least-256-bits-long-for-hs512-algorithm!";
    private final long expiration = 86400000; // 24 hours

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(secret, expiration);
    }

    // ==================== 正常生成与解析 ====================
    @Test
    @DisplayName("should generate non-null token containing username")
    void shouldGenerateNonNullTokenContainingUsername() {
        String token = jwtTokenProvider.generateToken("admin");

        assertThat(token).isNotBlank();
        assertThat(token.split("\\.")).hasSize(3); // JWT 三段结构
    }

    @Test
    @DisplayName("should extract correct username from valid token")
    void shouldExtractCorrectUsernameFromValidToken() {
        String token = jwtTokenProvider.generateToken("student01");

        String username = jwtTokenProvider.getUsername(token);

        assertThat(username).isEqualTo("student01");
    }

    @Test
    @DisplayName("should validate token as true when token is valid and not expired")
    void shouldValidateTokenAsTrueWhenValid() {
        String token = jwtTokenProvider.generateToken("admin");

        boolean valid = jwtTokenProvider.validateToken(token);

        assertThat(valid).isTrue();
    }

    // ==================== T-AUTH-06: 过期token ====================
    @Test
    @DisplayName("should validate expired token as false")
    void shouldValidateExpiredTokenAsFalse() {
        // 使用极短的过期时间构造一个已立即过期的 token
        JwtTokenProvider shortLivedProvider = new JwtTokenProvider(secret, -1000);
        String expiredToken = shortLivedProvider.generateToken("admin");

        boolean valid = jwtTokenProvider.validateToken(expiredToken);

        assertThat(valid).isFalse();
    }

    // ==================== 异常路径：无效token ====================
    @Test
    @DisplayName("should validate malformed token as false")
    void shouldValidateMalformedTokenAsFalse() {
        boolean valid = jwtTokenProvider.validateToken("not.a.jwt");
        assertThat(valid).isFalse();
    }

    @Test
    @DisplayName("should validate empty token as false")
    void shouldValidateEmptyTokenAsFalse() {
        boolean valid = jwtTokenProvider.validateToken("");
        assertThat(valid).isFalse();
    }

    @Test
    @DisplayName("should validate token signed with wrong secret as false")
    void shouldValidateTokenSignedWithWrongSecretAsFalse() {
        Key wrongKey = Keys.secretKeyFor(SignatureAlgorithm.HS512);
        String wrongToken = Jwts.builder()
                .setSubject("admin")
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(wrongKey)
                .compact();

        boolean valid = jwtTokenProvider.validateToken(wrongToken);
        assertThat(valid).isFalse();
    }

    // ==================== expiresIn ====================
    @Test
    @DisplayName("should return positive expiration time")
    void shouldReturnPositiveExpirationTime() {
        assertThat(jwtTokenProvider.getExpiration()).isEqualTo(expiration);
    }
}
