package com.t3a.core.controller;

import com.t3a.common.domain.Result;
import com.t3a.core.domain.dto.LoginRequest;
import com.t3a.core.domain.dto.RegisterRequest;
import com.t3a.core.domain.vo.LoginResponse;
import com.t3a.core.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "用户认证接口")
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        try {
            LoginResponse response = authService.login(request);
            return Result.success("登录成功", response);
        } catch (Exception e) {
            log.error("登录失败: {}", e.getMessage(), e);
            return Result.error("用户名或密码错误");
        }
    }

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public Result<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        try {
            LoginResponse response = authService.register(request);
            return Result.success("注册成功", response);
        } catch (Exception e) {
            log.error("注册失败: {}", e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    @Operation(summary = "刷新Token")
    @PostMapping("/refresh")
    public Result<LoginResponse> refreshToken(@RequestHeader("Refresh-Token") String refreshToken) {
        try {
            LoginResponse response = authService.refreshToken(refreshToken);
            return Result.success("刷新成功", response);
        } catch (Exception e) {
            log.error("刷新Token失败: {}", e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    @Operation(summary = "测试认证")
    @GetMapping("/test")
    public Result<String> test() {
        return Result.success("认证成功");
    }
}
