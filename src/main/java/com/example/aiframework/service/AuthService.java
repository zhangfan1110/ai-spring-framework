package com.example.aiframework.service;

import com.example.aiframework.dto.LoginRequest;
import com.example.aiframework.dto.LoginResponse;
import com.example.aiframework.entity.UserEntity;
import com.example.aiframework.mapper.UserMapper;
import com.example.aiframework.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 认证服务
 */
@Service
public class AuthService {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserMapper userMapper;

    /**
     * 用户登录
     */
    public LoginResponse login(LoginRequest request) {
        // 认证
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                request.getUsername(),
                request.getPassword()
            )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 查询用户信息
        UserEntity user = userMapper.findByUsername(request.getUsername());
        
        // 加载用户角色和权限
        List<String> roles = userMapper.findRoleCodesByUserId(user.getId());
        List<String> permissions = userMapper.findPermissionCodesByUserId(user.getId());

        // 生成 Token
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", roles);
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), claims);

        // 构建响应
        LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo();
        userInfo.setId(user.getId());
        userInfo.setUsername(user.getUsername());
        userInfo.setNickname(user.getNickname());
        userInfo.setAvatar(user.getAvatar());
        userInfo.setRoles(roles);
        userInfo.setPermissions(permissions);

        return new LoginResponse(token, userInfo);
    }

    /**
     * 登出
     */
    public void logout() {
        SecurityContextHolder.clearContext();
    }

    /**
     * 获取当前用户信息
     */
    public UserEntity getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        
        String username = authentication.getName();
        return userMapper.findByUsername(username);
    }

    /**
     * 获取当前用户 ID
     */
    public String getCurrentUserId() {
        UserEntity user = getCurrentUser();
        return user != null ? user.getId() : null;
    }
}
