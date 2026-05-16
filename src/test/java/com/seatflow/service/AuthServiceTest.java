package com.seatflow;

import com.seatflow.dto.request.LoginRequest;
import com.seatflow.entity.User;
import com.seatflow.mapper.PermissionMapper;
import com.seatflow.mapper.RoleMapper;
import com.seatflow.mapper.UserMapper;
import com.seatflow.security.JwtTokenProvider;
import com.seatflow.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService 单元测试")
class AuthServiceTest {

    @Mock private UserMapper userMapper;
    @Mock private RoleMapper roleMapper;
    @Mock private PermissionMapper permissionMapper;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    private User testUser;

    @BeforeEach
    void setUp() {
        // Set jwtExpiration field
        ReflectionTestUtils.setField(authService, "jwtExpiration", 86400000L);

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("admin");
        testUser.setPassword("$2a$10$encoded_password");
        testUser.setRealName("管理员");
        testUser.setEmail("admin@seatflow.edu");
        testUser.setUserType("ADMIN");
    }

    @Test
    @DisplayName("should return token when login with correct credentials")
    void shouldReturnToken_whenLoginWithCorrectCredentials() {
        // Arrange
        when(userMapper.selectOne(any())).thenReturn(testUser);
        when(passwordEncoder.matches("admin123", testUser.getPassword())).thenReturn(true);
        when(jwtTokenProvider.generateToken(1L, "admin", List.of("ADMIN"))).thenReturn("jwt-token-xxx");

        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("admin123");

        // Act
        var result = authService.login(request);

        // Assert
        assertNotNull(result);
        assertEquals("jwt-token-xxx", result.getToken());
        assertEquals(86400000L, result.getExpiresIn());
    }

    @Test
    @DisplayName("should throw 401 when login with wrong password")
    void shouldThrow401_whenLoginWithWrongPassword() {
        when(userMapper.selectOne(any())).thenReturn(testUser);
        when(passwordEncoder.matches("wrongpwd", testUser.getPassword())).thenReturn(false);

        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("wrongpwd");

        var ex = assertThrows(com.seatflow.common.exception.BusinessException.class,
                () -> authService.login(request));
        assertEquals(401, ex.getCode());
    }

    @Test
    @DisplayName("should throw 401 when login with non-existent username")
    void shouldThrow401_whenLoginWithNonExistentUsername() {
        when(userMapper.selectOne(any())).thenReturn(null);

        LoginRequest request = new LoginRequest();
        request.setUsername("nouser");
        request.setPassword("anypwd");

        var ex = assertThrows(com.seatflow.common.exception.BusinessException.class,
                () -> authService.login(request));
        assertEquals(401, ex.getCode());
    }
}
