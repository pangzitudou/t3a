package com.t3a.core.service;

import com.t3a.core.domain.dto.LoginRequest;
import com.t3a.core.domain.dto.RegisterRequest;
import com.t3a.core.domain.entity.User;
import com.t3a.core.domain.vo.LoginResponse;
import com.t3a.core.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 认证服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserService userService;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    /**
     * 用户登录
     */
    public LoginResponse login(LoginRequest request) {
        log.info("用户登录: {}", request.getUsername());

        // 认证
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        // 查询用户
        User user = userService.findByUsername(request.getUsername());
        UserDetails userDetails = userService.loadUserByUsername(request.getUsername());

        // 生成 Token
        String accessToken = jwtService.generateToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getJwtExpiration() / 1000)
                .userInfo(LoginResponse.UserInfo.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .nickname(user.getNickname())
                        .email(user.getEmail())
                        .avatar(user.getAvatar())
                        .role(user.getRole())
                        .build())
                .build();
    }

    /**
     * 用户注册
     */
    @Transactional(rollbackFor = Exception.class)
    public LoginResponse register(RegisterRequest request) {
        log.info("用户注册: {}", request.getUsername());

        // 检查用户名是否存在
        if (userService.existsByUsername(request.getUsername())) {
            throw new RuntimeException("用户名已存在");
        }

        // 检查邮箱是否存在
        if (request.getEmail() != null && userService.existsByEmail(request.getEmail())) {
            throw new RuntimeException("邮箱已被使用");
        }

        // 创建用户
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(request.getPassword());
        user.setEmail(request.getEmail());
        user.setNickname(request.getNickname());
        user = userService.createUser(user);

        // 生成 Token 并返回
        UserDetails userDetails = userService.loadUserByUsername(user.getUsername());
        String accessToken = jwtService.generateToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getJwtExpiration() / 1000)
                .userInfo(LoginResponse.UserInfo.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .nickname(user.getNickname())
                        .email(user.getEmail())
                        .avatar(user.getAvatar())
                        .role(user.getRole())
                        .build())
                .build();
    }

    /**
     * 刷新 Token
     */
    public LoginResponse refreshToken(String refreshToken) {
        String username = jwtService.extractUsername(refreshToken);
        User user = userService.findByUsername(username);
        UserDetails userDetails = userService.loadUserByUsername(username);

        if (!jwtService.isTokenValid(refreshToken, userDetails)) {
            throw new RuntimeException("刷新令牌无效");
        }

        String newAccessToken = jwtService.generateToken(userDetails);
        String newRefreshToken = jwtService.generateRefreshToken(userDetails);

        return LoginResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getJwtExpiration() / 1000)
                .userInfo(LoginResponse.UserInfo.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .nickname(user.getNickname())
                        .email(user.getEmail())
                        .avatar(user.getAvatar())
                        .role(user.getRole())
                        .build())
                .build();
    }
}
