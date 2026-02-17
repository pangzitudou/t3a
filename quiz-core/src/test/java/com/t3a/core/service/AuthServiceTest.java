package com.t3a.core.service;

import com.t3a.core.domain.dto.LoginRequest;
import com.t3a.core.domain.dto.RegisterRequest;
import com.t3a.core.domain.entity.User;
import com.t3a.core.domain.vo.LoginResponse;
import com.t3a.core.mapper.UserMapper;
import com.t3a.core.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 认证服务测试
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuthServiceTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JwtService jwtService;

    private RegisterRequest registerRequest;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setUsername("testuser");
        registerRequest.setPassword("test123456");
        registerRequest.setEmail("test@example.com");
        registerRequest.setNickname("测试用户");
    }

    @Test
    void testRegister_Success() {
        // 执行注册
        LoginResponse response = authService.register(registerRequest);

        // 验证响应
        assertNotNull(response);
        assertNotNull(response.getAccessToken());
        assertNotNull(response.getRefreshToken());
        assertEquals("Bearer", response.getTokenType());
        assertNotNull(response.getUserInfo());
        assertEquals("testuser", response.getUserInfo().getUsername());
        assertEquals("测试用户", response.getUserInfo().getNickname());

        // 验证用户已创建
        User user = userMapper.selectById(response.getUserInfo().getId());
        assertNotNull(user);
        assertEquals("testuser", user.getUsername());
        assertEquals(1, user.getStatus());
        assertEquals("USER", user.getRole());
    }

    @Test
    void testRegister_DuplicateUsername() {
        // 第一次注册成功
        authService.register(registerRequest);

        // 第二次注册应该失败
        assertThrows(RuntimeException.class, () -> {
            authService.register(registerRequest);
        });
    }

    @Test
    void testLogin_Success() {
        // 先注册用户
        authService.register(registerRequest);

        // 登录
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("test123456");

        LoginResponse response = authService.login(loginRequest);

        // 验证响应
        assertNotNull(response);
        assertNotNull(response.getAccessToken());
        assertNotNull(response.getRefreshToken());
        assertEquals("testuser", response.getUserInfo().getUsername());
    }

    @Test
    void testLogin_WrongPassword() {
        // 先注册用户
        authService.register(registerRequest);

        // 使用错误密码登录
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("wrongpassword");

        assertThrows(Exception.class, () -> {
            authService.login(loginRequest);
        });
    }

    @Test
    void testRefreshToken_Success() {
        // 注册并获取Token
        LoginResponse registerResponse = authService.register(registerRequest);
        String refreshToken = registerResponse.getRefreshToken();

        // 刷新Token
        LoginResponse refreshResponse = authService.refreshToken(refreshToken);

        // 验证新Token
        assertNotNull(refreshResponse);
        assertNotNull(refreshResponse.getAccessToken());
        assertFalse(refreshResponse.getAccessToken().isBlank());
    }
}
