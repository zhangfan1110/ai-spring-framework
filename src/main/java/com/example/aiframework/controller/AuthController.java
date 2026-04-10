package com.example.aiframework.controller;

import com.example.aiframework.system.dto.LoginRequest;
import com.example.aiframework.system.dto.LoginResponse;
import com.example.aiframework.system.entity.UserEntity;
import com.example.aiframework.system.service.AuthService;
import com.example.aiframework.common.util.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证控制器
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "认证管理", description = "用户登录、登出、权限验证")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Operation(summary = "用户登录", description = "用户名密码登录，返回 JWT Token")
    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody LoginRequest request) {
        try {
            LoginResponse response = authService.login(request);
            return Result.success(response);
        } catch (Exception e) {
            return Result.error("登录失败：" + e.getMessage());
        }
    }

    @Operation(summary = "用户登出")
    @PostMapping("/logout")
    public Result<String> logout() {
        authService.logout();
        return Result.success("登出成功");
    }

    @Operation(summary = "获取当前用户信息")
    @GetMapping("/me")
    public Result<Map<String, Object>> getCurrentUser() {
        UserEntity user = authService.getCurrentUser();
        if (user == null) {
            return Result.error("未登录");
        }

        Map<String, Object> info = new HashMap<>();
        info.put("id", user.getId());
        info.put("username", user.getUsername());
        info.put("nickname", user.getNickname());
        info.put("email", user.getEmail());
        info.put("phone", user.getPhone());
        info.put("avatar", user.getAvatar());

        return Result.success(info);
    }

    @Operation(summary = "验证 Token 是否有效")
    @GetMapping("/validate")
    public Result<Map<String, Object>> validateToken(@RequestHeader("Authorization") String authorization) {
        Map<String, Object> result = new HashMap<>();
        
        if (authorization != null && authorization.startsWith("Bearer ")) {
            String token = authorization.substring(7);
            result.put("valid", true);
            result.put("message", "Token 有效");
            return Result.success(result);
        }
        
        result.put("valid", false);
        result.put("message", "Token 无效");
        return Result.success(result);
    }
}
